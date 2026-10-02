package mc.item;

public final class ItemStack {
    public final Item item;
    public int count;
    public int damage;
    /** Enchantments and their levels (null = none). */
    public java.util.EnumMap<Enchantment, Integer> enchants;
    /** Mods' data components (null = none). */
    public ItemComponents components;

    public ItemStack(Item item, int count) {
        this(item, count, 0);
    }

    public ItemStack(Item item, int count, int damage) {
        this.item = item;
        this.count = count;
        this.damage = damage;
    }

    public ItemStack copy() {
        ItemStack s = new ItemStack(item, count, damage);
        if (enchants != null) s.enchants = new java.util.EnumMap<>(enchants);
        if (components != null) s.components = components.copy();
        return s;
    }

    public ItemStack split(int n) {
        n = Math.min(n, count);
        count -= n;
        ItemStack s = new ItemStack(item, n, damage);
        if (enchants != null) s.enchants = new java.util.EnumMap<>(enchants);
        if (components != null) s.components = components.copy();
        return s;
    }

    public boolean canMerge(ItemStack o) {
        return o != null && o.item == item && o.damage == damage && item.maxStack > 1 && !o.isEnchanted() && !isEnchanted()
                && sameComponents(o);
    }

    public boolean sameComponents(ItemStack o) {
        boolean a = components == null || components.isEmpty(), b = o.components == null || o.components.isEmpty();
        if (a || b) return a == b;
        return components.equals(o.components);
    }

    public boolean isEnchanted() { return enchants != null && !enchants.isEmpty(); }

    public int level(Enchantment e) {
        if (enchants == null) return 0;
        return enchants.getOrDefault(e, 0);
    }

    public static int level(ItemStack s, Enchantment e) { return s == null ? 0 : s.level(e); }

    public void enchant(Enchantment e, int level) {
        if (enchants == null) enchants = new java.util.EnumMap<>(Enchantment.class);
        enchants.put(e, level);
    }

    /**
     * Serialised form: [id, count, damage, (enchantment ordinal, level)...], then for mod components
     * [-1, byte length, UTF-8 bytes packed four to an int...].
     */
    public int[] toArray() {
        int n = enchants == null ? 0 : enchants.size();
        byte[] extra = components == null || components.isEmpty() ? null : components.save().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int[] a = new int[3 + n * 2 + (extra == null ? 0 : 2 + (extra.length + 3) / 4)];
        a[0] = item.id; a[1] = count; a[2] = damage;
        int i = 3;
        if (enchants != null) for (var e : enchants.entrySet()) { a[i++] = e.getKey().ordinal(); a[i++] = e.getValue(); }
        if (extra != null) {
            a[i++] = -1;
            a[i++] = extra.length;
            for (int k = 0; k < extra.length; k++) a[i + k / 4] |= (extra[k] & 255) << (8 * (k % 4));
        }
        return a;
    }

    public static ItemStack fromArray(int[] a) {
        if (a == null || a.length < 3 || Item.get(a[0]) == null) return null;
        ItemStack s = new ItemStack(Item.get(a[0]), a[1], a[2]);
        for (int i = 3; i + 1 < a.length; i += 2) {
            if (a[i] == -1) {
                int len = a[i + 1];
                byte[] extra = new byte[len];
                for (int k = 0; k < len && i + 2 + k / 4 < a.length; k++) extra[k] = (byte) (a[i + 2 + k / 4] >> (8 * (k % 4)));
                s.components = ItemComponents.parse(new String(extra, java.nio.charset.StandardCharsets.UTF_8));
                break;
            }
            if (a[i] >= 0 && a[i] < Enchantment.values().length) s.enchant(Enchantment.values()[a[i]], a[i + 1]);
        }
        return s;
    }

    public static boolean isEmpty(ItemStack s) { return s == null || s.count <= 0; }

    private static final java.util.Random RANDOM = new java.util.Random();

    /** Applies tool wear (Unbreaking can skip it); returns true when the tool broke. */
    public boolean damageTool(int amount) {
        if (item.maxDamage <= 0) return false;
        int unbreaking = level(Enchantment.UNBREAKING);
        if (unbreaking > 0) {
            int applied = 0;
            for (int i = 0; i < amount; i++) {
                boolean skip = item.isArmor() ? RANDOM.nextFloat() >= 0.6f + 0.4f / (unbreaking + 1) : RANDOM.nextInt(unbreaking + 1) > 0;
                if (!skip) applied++;
            }
            amount = applied;
        }
        damage += amount;
        if (damage >= item.maxDamage) {
            count = 0;
            return true;
        }
        return false;
    }
}
