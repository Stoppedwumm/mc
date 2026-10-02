package net.minecraft.world.item.component;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;

/** The effects a suspicious stew gives (reamc-compat). */
public record SuspiciousStewEffects(List<Entry> effects) {
    public static final SuspiciousStewEffects EMPTY = new SuspiciousStewEffects(List.of());

    public SuspiciousStewEffects withEffectAdded(Entry e) {
        java.util.ArrayList<Entry> l = new java.util.ArrayList<>(effects);
        l.add(e);
        return new SuspiciousStewEffects(List.copyOf(l));
    }

    public record Entry(Holder<MobEffect> effect, int duration) {
        public MobEffectInstance createEffectInstance() { return new MobEffectInstance(effect, duration); }
    }
}
