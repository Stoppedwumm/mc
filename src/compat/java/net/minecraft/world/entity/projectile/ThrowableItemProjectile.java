package net.minecraft.world.entity.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A thrown item, drawn as the item's sprite (reamc-compat). */
public abstract class ThrowableItemProjectile extends ThrowableProjectile implements ItemSupplier {
    private ItemStack item = ItemStack.EMPTY;

    public ThrowableItemProjectile(EntityType<? extends ThrowableItemProjectile> type, Level level) { super(type, level); }

    public ThrowableItemProjectile(EntityType<? extends ThrowableItemProjectile> type, double x, double y, double z, Level level) { super(type, x, y, z, level); }

    public ThrowableItemProjectile(EntityType<? extends ThrowableItemProjectile> type, LivingEntity shooter, Level level) { super(type, shooter, level); }

    public void setItem(ItemStack stack) { item = stack.copyWithCount(1); }

    protected abstract Item getDefaultItem();

    @Override
    public ItemStack getItem() { return item.isEmpty() ? new ItemStack(getDefaultItem()) : item; }
}
