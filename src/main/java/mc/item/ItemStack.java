package mc.item;

public final class ItemStack {
    public final Item item;
    public int count;
    public int damage;

    public ItemStack(Item item, int count) {
        this(item, count, 0);
    }

    public ItemStack(Item item, int count, int damage) {
        this.item = item;
        this.count = count;
        this.damage = damage;
    }

    public ItemStack copy() { return new ItemStack(item, count, damage); }

    public ItemStack split(int n) {
        n = Math.min(n, count);
        count -= n;
        return new ItemStack(item, n, damage);
    }

    public boolean canMerge(ItemStack o) {
        return o != null && o.item == item && o.damage == damage && item.maxStack > 1;
    }

    public static boolean isEmpty(ItemStack s) { return s == null || s.count <= 0; }

    /** Applies tool wear; returns true when the tool broke. */
    public boolean damageTool(int amount) {
        if (item.maxDamage <= 0) return false;
        damage += amount;
        if (damage >= item.maxDamage) {
            count = 0;
            return true;
        }
        return false;
    }
}
