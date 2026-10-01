package net.minecraft.world.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

/**
 * A stack of items. reamc-compat wraps the engine's stack, so a mod changing a stack's count changes the stack in the
 * player's inventory or container it came from.
 */
public final class ItemStack {
    public static final ItemStack EMPTY = new ItemStack((mc.item.ItemStack) null);
    private final mc.item.ItemStack handle;

    public ItemStack(ItemLike item) { this(item, 1); }

    public ItemStack(ItemLike item, int count) {
        Item i = item == null ? null : item.asItem();
        mc.item.Item engine = i == null ? null : mc.mod.Bridge.engineItem(i);
        handle = engine == null || count <= 0 ? null : new mc.item.ItemStack(engine, count);
    }

    public ItemStack(net.minecraft.core.Holder<Item> item) { this(item.value(), 1); }

    private ItemStack(mc.item.ItemStack handle) { this.handle = handle; }

    /** The compat view of an engine stack (shares it). */
    public static ItemStack reamc$wrap(mc.item.ItemStack h) { return h == null || h.count <= 0 || h.item == null ? EMPTY : new ItemStack(h); }

    /** The engine stack behind this one, or null when empty. */
    public mc.item.ItemStack reamc$handle() { return isEmpty() ? null : handle; }

    public boolean isEmpty() { return handle == null || handle.count <= 0 || handle.item == null; }

    public Item getItem() { return isEmpty() ? Items.AIR : mc.mod.Bridge.compatItem(handle.item); }

    public boolean is(Item item) { return getItem() == item; }

    public int getCount() { return isEmpty() ? 0 : handle.count; }

    public void setCount(int n) { if (handle != null) handle.count = n; }

    public void grow(int n) { setCount(getCount() + n); }

    public void shrink(int n) { setCount(getCount() - n); }

    public void limitSize(int max) { if (getCount() > max) setCount(max); }

    public ItemStack split(int n) {
        int k = Math.min(n, getCount());
        ItemStack s = copyWithCount(k);
        shrink(k);
        return s;
    }

    public ItemStack copy() { return isEmpty() ? EMPTY : new ItemStack(handle.copy()); }

    public ItemStack copyWithCount(int n) {
        if (isEmpty() || n <= 0) return EMPTY;
        mc.item.ItemStack c = handle.copy();
        c.count = n;
        return new ItemStack(c);
    }

    public ItemStack copyAndClear() {
        ItemStack c = copy();
        setCount(0);
        return c;
    }

    public int getMaxStackSize() { return isEmpty() ? 64 : handle.item.maxStack; }

    public boolean isStackable() { return getMaxStackSize() > 1 && (!isDamageableItem() || !isDamaged()); }

    public boolean isDamageableItem() { return !isEmpty() && handle.item.maxDamage > 0; }

    public boolean isDamaged() { return !isEmpty() && handle.damage > 0; }

    public int getDamageValue() { return isEmpty() ? 0 : handle.damage; }

    public void setDamageValue(int d) { if (handle != null) handle.damage = Math.max(0, d); }

    public int getMaxDamage() { return isEmpty() ? 0 : handle.item.maxDamage; }

    public boolean isEnchanted() { return !isEmpty() && handle.isEnchanted(); }

    public Component getHoverName() { return Component.literal(isEmpty() ? "Air" : mc.item.Item.displayName(handle)); }

    public Component getDisplayName() { return Component.literal("[" + getHoverName().getString() + "]"); }

    public <T> T getCapability(net.neoforged.neoforge.capabilities.ItemCapability<T, Void> capability) { return null; }

    public <T, C> T getCapability(net.neoforged.neoforge.capabilities.ItemCapability<T, C> capability, C context) { return null; }

    public static boolean isSameItem(ItemStack a, ItemStack b) { return a.getItem() == b.getItem(); }

    public static boolean isSameItemSameComponents(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return a.isEmpty() && b.isEmpty();
        return a.handle.canMerge(b.handle);
    }

    public static boolean matches(ItemStack a, ItemStack b) {
        return a == b || (a.getCount() == b.getCount() && isSameItemSameComponents(a, b));
    }

    /** Saved form: Minecraft-style id and count, plus reamc's full stack data. */
    public CompoundTag reamc$save() {
        CompoundTag c = new CompoundTag();
        if (isEmpty()) return c;
        c.putString("id", handle.item.key());
        c.putInt("count", handle.count);
        c.putIntArray("reamc", handle.toArray());
        return c;
    }

    public static ItemStack reamc$load(CompoundTag c) {
        if (c.contains("reamc")) return reamc$wrap(mc.item.ItemStack.fromArray(c.getIntArray("reamc")));
        mc.item.Item it = mc.item.Item.byKey(c.getString("id"));
        return it == null ? EMPTY : reamc$wrap(new mc.item.ItemStack(it, Math.max(1, c.getInt("count"))));
    }

    @Override public String toString() { return getCount() + " " + getItem(); }
}
