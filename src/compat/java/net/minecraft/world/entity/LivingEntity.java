package net.minecraft.world.entity;

import net.minecraft.world.item.ItemStack;

public abstract class LivingEntity extends Entity {
    protected LivingEntity(mc.entity.LivingEntity engine) { super(engine); }

    private mc.entity.LivingEntity living() { return (mc.entity.LivingEntity) reamc$entity; }

    public float getHealth() { return living().health; }
    public float getMaxHealth() { return living().maxHealth; }
    public void setHealth(float h) { living().health = Math.max(0, Math.min(getMaxHealth(), h)); }
    public void heal(float amount) { living().heal(amount); }
    public boolean isDeadOrDying() { return living().isDead(); }
    public ItemStack getMainHandItem() { return ItemStack.EMPTY; }
    public ItemStack getOffhandItem() { return ItemStack.EMPTY; }
}
