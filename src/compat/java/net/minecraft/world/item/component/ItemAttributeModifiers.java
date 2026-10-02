package net.minecraft.world.item.component;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.text.DecimalFormat;
import java.util.List;

/** Attribute bonuses of an item (reamc-compat: reamc uses the main-hand attack damage and speed). */
public record ItemAttributeModifiers(List<Entry> modifiers, boolean showInTooltip) {
    public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(List.of(), true);
    public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = new DecimalFormat("#.##");

    public static Builder builder() { return new Builder(); }

    public ItemAttributeModifiers withTooltip(boolean show) { return new ItemAttributeModifiers(modifiers, show); }

    public ItemAttributeModifiers withModifierAdded(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
        ImmutableList.Builder<Entry> b = ImmutableList.builder();
        for (Entry e : modifiers) if (!e.matches(attribute, modifier.id())) b.add(e);
        b.add(new Entry(attribute, modifier, slot));
        return new ItemAttributeModifiers(b.build(), showInTooltip);
    }

    public double compute(double base, net.minecraft.world.entity.EquipmentSlot slot) {
        double v = base;
        for (Entry e : modifiers) {
            if (!e.slot().test(slot)) continue;
            double a = e.modifier().amount();
            v += switch (e.modifier().operation()) {
                case ADD_VALUE -> a;
                case ADD_MULTIPLIED_BASE -> a * base;
                case ADD_MULTIPLIED_TOTAL -> 0;
            };
        }
        for (Entry e : modifiers) if (e.slot().test(slot) && e.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) v *= 1 + e.modifier().amount();
        return v;
    }

    private float sum(Holder<Attribute> attribute) {
        float s = 0;
        for (Entry e : modifiers) if (e.attribute().value() == attribute.value() && e.slot().test(net.minecraft.world.entity.EquipmentSlot.MAINHAND)) s += (float) e.modifier().amount();
        return s;
    }

    /** Damage added to the hand's 1 point. */
    public float reamc$attackDamage() { return sum(Attributes.ATTACK_DAMAGE); }

    /** Change to the base 4 attacks per second. */
    public float reamc$attackSpeed() { return sum(Attributes.ATTACK_SPEED); }

    public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
        public boolean matches(Holder<Attribute> a, net.minecraft.resources.ResourceLocation id) { return a.value() == attribute.value() && modifier.is(id); }
    }

    public static class Builder {
        private final ImmutableList.Builder<Entry> entries = ImmutableList.builder();

        Builder() { }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
            entries.add(new Entry(attribute, modifier, slot));
            return this;
        }

        public ItemAttributeModifiers build() { return new ItemAttributeModifiers(entries.build(), true); }
    }
}
