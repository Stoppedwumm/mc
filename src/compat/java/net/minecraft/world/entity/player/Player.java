package net.minecraft.world.entity.player;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.OptionalInt;

/** A player as mods see it, backed by reamc's player (reamc-compat). */
public abstract class Player extends LivingEntity {
    private final Inventory inventory;
    private final FoodData foodData;
    private final Abilities abilities = new Abilities();
    public final InventoryMenu inventoryMenu;
    /** The open menu (the player's own inventory menu when nothing else is open). */
    public AbstractContainerMenu containerMenu;

    protected Player(mc.entity.Player engine) {
        super(engine);
        inventory = new Inventory(this);
        foodData = new FoodData(engine);
        inventoryMenu = new InventoryMenu(inventory, true, this);
        containerMenu = inventoryMenu;
    }

    public mc.entity.Player reamc$player() { return (mc.entity.Player) reamc$entity; }

    public Inventory getInventory() { return inventory; }

    public FoodData getFoodData() { return foodData; }

    public Abilities getAbilities() {
        abilities.instabuild = abilities.mayfly = abilities.invulnerable = isCreative();
        abilities.flying = reamc$player().flying;
        return abilities;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) return ItemStack.reamc$wrap(reamc$player().inventory.held());
        if (slot == EquipmentSlot.OFFHAND) return ItemStack.EMPTY;
        return super.getItemBySlot(slot);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) reamc$player().inventory.setHeld(stack.isEmpty() ? null : stack.reamc$handle());
        else if (slot == EquipmentSlot.OFFHAND) { if (!stack.isEmpty() && !inventory.add(stack)) drop(stack, false); }
        else super.setItemSlot(slot, stack);
    }

    public boolean isCreative() { return reamc$player().creative; }
    public boolean hasCorrectToolForDrops(net.minecraft.world.level.block.state.BlockState state) { return !state.requiresCorrectToolForDrops() || getMainHandItem().isCorrectToolForDrops(state); }
    public float getDestroySpeed(net.minecraft.world.level.block.state.BlockState state) { return Math.max(1, getMainHandItem().getDestroySpeed(state)); }
    @Override public boolean isSpectator() { return false; }
    public boolean isHurt() { return getHealth() > 0 && getHealth() < getMaxHealth(); }
    public boolean isSecondaryUseActive() { return isShiftKeyDown(); }
    public boolean canEat(boolean alwaysEdible) { return alwaysEdible || isCreative() || foodData.needsFood(); }
    public boolean mayBuild() { return true; }
    public boolean mayUseItemAt(BlockPos pos, net.minecraft.core.Direction face, ItemStack stack) { return true; }
    public boolean hasInfiniteMaterials() { return isCreative(); }
    public boolean isLocalPlayer() { return false; }
    public boolean isModelPartShown(Object part) { return true; }

    @Override public Component getName() { return Component.literal(reamc$player().name); }

    public ItemEntity drop(ItemStack stack, boolean randomly) { return drop(stack, randomly, false); }

    public ItemEntity drop(ItemStack stack, boolean randomly, boolean includeThrower) {
        if (stack.isEmpty()) return null;
        mc.entity.Player p = reamc$player();
        mc.entity.ItemEntity e = new mc.entity.ItemEntity(stack.reamc$handle().copy());
        e.setPos(p.x, p.eyeY() - 0.3, p.z);
        double r = Math.toRadians(p.yaw);
        e.motionX = -Math.sin(r) * 0.3;
        e.motionZ = Math.cos(r) * 0.3;
        e.pickupDelay = 40;
        p.world.addEntity(e);
        return (ItemEntity) mc.mod.Bridge.wrap(e);
    }

    public void sendSystemMessage(Component message) { mc.mod.Bridge.chat(reamc$player(), message.getString()); }

    public void displayClientMessage(Component message, boolean actionBar) { mc.mod.Bridge.chat(reamc$player(), message.getString()); }

    public void giveExperiencePoints(int xp) { reamc$player().addXp(xp); }

    public void giveExperienceLevels(int levels) { for (int i = 0; i < levels; i++) reamc$player().addXp(mc.entity.Player.xpBarCap(reamc$player().xpLevel)); }

    public boolean addItem(ItemStack stack) { return inventory.add(stack); }

    public void closeContainer() { mc.mod.Bridge.closeMenu(this); }

    public OptionalInt openMenu(MenuProvider provider) { return OptionalInt.empty(); }

    public OptionalInt openMenu(MenuProvider provider, BlockPos pos) { return OptionalInt.empty(); }

    public void awardStat(net.minecraft.stats.Stat<?> stat) { }

    public void awardStat(net.minecraft.stats.Stat<?> stat, int amount) { }

    public void awardStat(net.minecraft.resources.ResourceLocation stat) { }

    public int awardRecipes(Collection<?> recipes) { return recipes.size(); }

    public void causeFoodExhaustion(float f) { reamc$player().addExhaustion(f); }

    public net.minecraft.world.item.ItemCooldowns getCooldowns() { return mc.mod.Bridge.cooldowns(this); }

    @Override
    public net.minecraft.world.item.ItemStack eat(net.minecraft.world.level.Level level, ItemStack stack, net.minecraft.world.food.FoodProperties food) {
        foodData.eat(food);
        return super.eat(level, stack, food);
    }
}
