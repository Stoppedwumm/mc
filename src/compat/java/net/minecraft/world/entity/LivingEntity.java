package net.minecraft.world.entity;

import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/** A living entity: health, effects, held items (reamc-compat). */
public abstract class LivingEntity extends Entity {
    protected LivingEntity(mc.entity.LivingEntity engine) { super(engine); }

    protected LivingEntity(EntityType<? extends LivingEntity> type, Level level) { super(type, level); }

    private mc.entity.LivingEntity living() { return (mc.entity.LivingEntity) reamc$entity; }

    public float getHealth() { return living().health; }
    public float getMaxHealth() { return living().maxHealth; }
    public void setHealth(float h) { living().health = Math.max(0, Math.min(getMaxHealth(), h)); }
    public void heal(float amount) { living().heal(amount); }
    public boolean isDeadOrDying() { return living().isDead(); }
    public boolean isBaby() { return reamc$entity instanceof mc.entity.Mob m && m.isBaby(); }
    public int getArmorValue() { return living().armorValue(); }
    public float getAbsorptionAmount() { return 0; }
    public boolean isSleeping() { return false; }
    public void swing(InteractionHand hand) { if (reamc$entity instanceof mc.entity.Player p) p.swingTicks = 6; }
    public void swing(InteractionHand hand, boolean updateSelf) { swing(hand); }

    // ------------------------------------------------------------------ items in hands and armour

    public ItemStack getMainHandItem() { return getItemBySlot(EquipmentSlot.MAINHAND); }
    public ItemStack getOffhandItem() { return getItemBySlot(EquipmentSlot.OFFHAND); }
    public ItemStack getItemInHand(InteractionHand hand) { return hand == InteractionHand.MAIN_HAND ? getMainHandItem() : getOffhandItem(); }

    public void setItemInHand(InteractionHand hand, ItemStack stack) {
        setItemSlot(hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND, stack);
    }

    public ItemStack getItemBySlot(EquipmentSlot slot) {
        mc.item.ItemStack[] armor = living().armorSlots();
        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && armor != null) return ItemStack.reamc$wrap(armor[3 - slot.getIndex()]);
        return ItemStack.EMPTY;
    }

    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        mc.item.ItemStack[] armor = living().armorSlots();
        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && armor != null) armor[3 - slot.getIndex()] = stack.reamc$handle();
    }

    public static EquipmentSlot getSlotForHand(InteractionHand hand) { return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND; }

    public boolean isHolding(Item item) { return getMainHandItem().is(item) || getOffhandItem().is(item); }

    public void onEquippedItemBroken(Item item, EquipmentSlot slot) { }

    // ------------------------------------------------------------------ effects

    public boolean hasEffect(Holder<MobEffect> effect) {
        mc.entity.Effect e = effect.value().reamc$engine;
        if (e != null) return living().hasEffect(e);
        return mc.mod.Bridge.modEffects(living()).containsKey(effect.value());
    }

    public MobEffectInstance getEffect(Holder<MobEffect> effect) {
        mc.entity.Effect e = effect.value().reamc$engine;
        if (e != null) {
            mc.entity.Effect.Instance i = living().effects.get(e);
            return i == null ? null : new MobEffectInstance(effect, i.duration, i.amplifier);
        }
        return mc.mod.Bridge.modEffects(living()).get(effect.value());
    }

    public Collection<MobEffectInstance> getActiveEffects() {
        java.util.List<MobEffectInstance> l = new ArrayList<>();
        for (mc.entity.Effect.Instance i : living().effects.values()) {
            Holder<MobEffect> h = mc.mod.Bridge.effectHolder(i.effect);
            if (h != null) l.add(new MobEffectInstance(h, i.duration, i.amplifier));
        }
        l.addAll(mc.mod.Bridge.modEffects(living()).values());
        return l;
    }

    public Map<Holder<MobEffect>, MobEffectInstance> getActiveEffectsMap() {
        Map<Holder<MobEffect>, MobEffectInstance> m = new java.util.LinkedHashMap<>();
        for (MobEffectInstance i : getActiveEffects()) m.put(i.getEffect(), i);
        return m;
    }

    public boolean canBeAffected(MobEffectInstance i) { return true; }

    public boolean addEffect(MobEffectInstance i) { return addEffect(i, null); }

    public boolean addEffect(MobEffectInstance i, Entity source) {
        if (!canBeAffected(i)) return false;
        mc.entity.Effect e = i.getEffect().value().reamc$engine;
        if (e != null) {
            living().addEffect(e, i.getAmplifier(), i.isInfiniteDuration() ? Integer.MAX_VALUE : i.getDuration());
            return true;
        }
        Map<MobEffect, MobEffectInstance> m = mc.mod.Bridge.modEffects(living());
        MobEffectInstance old = m.get(i.getEffect().value());
        if (old == null) {
            m.put(i.getEffect().value(), new MobEffectInstance(i));
            i.onEffectAdded(this);
            i.onEffectStarted(this);
            return true;
        }
        return old.update(i);
    }

    public void forceAddEffect(MobEffectInstance i, Entity source) { addEffect(i, source); }

    public boolean removeEffect(Holder<MobEffect> effect) {
        mc.entity.Effect e = effect.value().reamc$engine;
        if (e != null) return living().effects.remove(e) != null;
        return mc.mod.Bridge.modEffects(living()).remove(effect.value()) != null;
    }

    public MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> effect) {
        MobEffectInstance i = getEffect(effect);
        removeEffect(effect);
        return i;
    }

    public boolean removeAllEffects() {
        boolean any = !getActiveEffects().isEmpty();
        living().clearEffects();
        mc.mod.Bridge.modEffects(living()).clear();
        return any;
    }

    public boolean removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCure cure) { return removeAllEffects(); }

    // ------------------------------------------------------------------ using items

    public ItemStack getUseItem() {
        return reamc$entity instanceof mc.entity.Player p && p.useStack != null ? ItemStack.reamc$wrap(p.useStack) : ItemStack.EMPTY;
    }

    public int getUseItemRemainingTicks() { return reamc$entity instanceof mc.entity.Player p && p.useStack != null ? p.useRemaining : 0; }

    public int getTicksUsingItem() {
        ItemStack s = getUseItem();
        return s.isEmpty() ? 0 : s.getUseDuration(this) - getUseItemRemainingTicks();
    }

    public boolean isUsingItem() { return !getUseItem().isEmpty(); }

    public InteractionHand getUsedItemHand() { return InteractionHand.MAIN_HAND; }

    public void startUsingItem(InteractionHand hand) {
        if (!(reamc$entity instanceof mc.entity.Player p) || hand != InteractionHand.MAIN_HAND) return;
        ItemStack s = getItemInHand(hand);
        if (s.isEmpty()) return;
        p.useStack = s.reamc$handle();
        p.useRemaining = s.getUseDuration(this);
    }

    public void stopUsingItem() { if (reamc$entity instanceof mc.entity.Player p) { p.useStack = null; p.useRemaining = 0; } }

    public void releaseUsingItem() {
        ItemStack s = getUseItem();
        if (!s.isEmpty()) s.releaseUsing(level(), this, getUseItemRemainingTicks());
        stopUsingItem();
    }

    /** Eats: restores hunger (players) and applies the food's effects. */
    public ItemStack eat(Level level, ItemStack stack, FoodProperties food) {
        if (!level.isClientSide) {
            for (FoodProperties.PossibleEffect pe : food.effects()) if (random.nextFloat() < pe.probability()) addEffect(pe.effect());
        }
        level.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.GENERIC_EAT, net.minecraft.sounds.SoundSource.NEUTRAL, 1, 1 + (random.nextFloat() - random.nextFloat()) * 0.4f);
        stack.consume(1, this);
        return stack;
    }

    public ItemStack eat(Level level, ItemStack stack) {
        FoodProperties food = stack.getFoodProperties(this);
        return food != null ? eat(level, stack, food) : stack;
    }

    public net.minecraft.sounds.SoundEvent getEatingSound(ItemStack stack) { return net.minecraft.sounds.SoundEvents.GENERIC_EAT; }

    public LivingEntity getLastHurtByMob() { return null; }

    public boolean hasLineOfSight(Entity e) { return true; }

    public double getAttributeValue(Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        return attribute.value().getDefaultValue();
    }
}
