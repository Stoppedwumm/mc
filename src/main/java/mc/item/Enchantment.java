package mc.item;

import java.util.*;

/** Enchantments with Minecraft's level ranges, power windows and rarity weights. */
public enum Enchantment {
    //            name                  max  weight  kind              minBase perLevel range
    PROTECTION("Protection", 4, 10, Kind.ARMOR, 1, 11, 11),
    FIRE_PROTECTION("Fire Protection", 4, 5, Kind.ARMOR, 10, 8, 8),
    FEATHER_FALLING("Feather Falling", 4, 5, Kind.FEET, 5, 6, 6),
    BLAST_PROTECTION("Blast Protection", 4, 2, Kind.ARMOR, 5, 8, 8),
    PROJECTILE_PROTECTION("Projectile Protection", 4, 5, Kind.ARMOR, 3, 6, 6),
    RESPIRATION("Respiration", 3, 2, Kind.HEAD, 10, 10, 30),
    THORNS("Thorns", 3, 1, Kind.CHEST, 10, 20, 50),
    SHARPNESS("Sharpness", 5, 10, Kind.WEAPON, 1, 11, 20),
    SMITE("Smite", 5, 5, Kind.WEAPON, 5, 8, 20),
    KNOCKBACK("Knockback", 2, 5, Kind.WEAPON, 5, 20, 50),
    FIRE_ASPECT("Fire Aspect", 2, 2, Kind.WEAPON, 10, 20, 50),
    LOOTING("Looting", 3, 2, Kind.WEAPON, 15, 9, 50),
    EFFICIENCY("Efficiency", 5, 10, Kind.TOOL, 1, 10, 50),
    SILK_TOUCH("Silk Touch", 1, 1, Kind.TOOL, 15, 0, 50),
    UNBREAKING("Unbreaking", 3, 5, Kind.BREAKABLE, 5, 8, 50),
    FORTUNE("Fortune", 3, 2, Kind.TOOL, 15, 9, 50),
    POWER("Power", 5, 10, Kind.BOW, 1, 10, 15),
    PUNCH("Punch", 2, 2, Kind.BOW, 12, 20, 25),
    FLAME("Flame", 1, 2, Kind.BOW, 20, 0, 30),
    INFINITY("Infinity", 1, 1, Kind.BOW, 20, 0, 30),
    LUCK_OF_THE_SEA("Luck of the Sea", 3, 2, Kind.ROD, 15, 9, 50),
    LURE("Lure", 3, 2, Kind.ROD, 15, 9, 50);

    public enum Kind { ARMOR, HEAD, CHEST, FEET, WEAPON, TOOL, BOW, ROD, BREAKABLE }

    public final String displayName;
    public final int maxLevel, weight;
    public final Kind kind;
    private final int minBase, perLevel, range;

    Enchantment(String displayName, int maxLevel, int weight, Kind kind, int minBase, int perLevel, int range) {
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.weight = weight;
        this.kind = kind;
        this.minBase = minBase;
        this.perLevel = perLevel;
        this.range = range;
    }

    public int minPower(int level) { return minBase + (level - 1) * perLevel; }

    public int maxPower(int level) { return minPower(level) + range; }

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V"};

    public static String roman(int level) { return level < ROMAN.length ? ROMAN[level] : String.valueOf(level); }

    public String describe(int level) {
        return maxLevel == 1 ? displayName : displayName + " " + (level < ROMAN.length ? ROMAN[level] : String.valueOf(level));
    }

    /** Whether the enchantment fits an item (books accept everything). */
    public boolean canApply(Item item) {
        if (item == Item.BOOK || item == Item.ENCHANTED_BOOK) return true;
        return switch (kind) {
            case ARMOR -> item.isArmor();
            case HEAD -> item.armorSlot == 0;
            case CHEST -> item.armorSlot == 1;
            case FEET -> item.armorSlot == 3;
            case WEAPON -> item.tool == Item.Tool.SWORD || (this == SHARPNESS || this == SMITE) && item.tool == Item.Tool.AXE;
            case TOOL -> item.tool == Item.Tool.PICKAXE || item.tool == Item.Tool.AXE || item.tool == Item.Tool.SHOVEL || item.tool == Item.Tool.HOE;
            case BOW -> item == Item.BOW;
            case ROD -> item == Item.FISHING_ROD;
            case BREAKABLE -> item.maxDamage > 0;
        };
    }

    /** Mutually exclusive groups, like Minecraft. */
    public boolean compatibleWith(Enchantment o) {
        if (o == this) return false;
        Set<Enchantment> prot = EnumSet.of(PROTECTION, FIRE_PROTECTION, BLAST_PROTECTION, PROJECTILE_PROTECTION);
        if (prot.contains(this) && prot.contains(o)) return false;
        if ((this == SHARPNESS || this == SMITE) && (o == SHARPNESS || o == SMITE)) return false;
        if ((this == SILK_TOUCH && o == FORTUNE) || (this == FORTUNE && o == SILK_TOUCH)) return false;
        return true;
    }

    /** How easily an item takes enchantments (Minecraft's enchantability). */
    public static int enchantability(Item item) {
        if (item == Item.BOOK) return 1;
        if (item.isArmor()) return new int[]{15, 12, 9, 25, 10}[item.armorMaterial];
        if (item == Item.BOW || item == Item.FISHING_ROD) return 1;
        if (item.isTool()) {
            if (item.name.startsWith("Wooden")) return 15;
            if (item.name.startsWith("Stone")) return 5;
            if (item.name.startsWith("Iron")) return 14;
            if (item.name.startsWith("Golden")) return 22;
            if (item.name.startsWith("Diamond")) return 10;
        }
        return 0;
    }

    /**
     * Rolls enchantments for an item at an enchanting-table level, following Minecraft's algorithm: a modified level
     * picks one weighted enchantment whose power window contains it, then more with decreasing probability.
     */
    public static Map<Enchantment, Integer> roll(Item item, int level, Random r) {
        Map<Enchantment, Integer> out = new EnumMap<>(Enchantment.class);
        int ench = enchantability(item);
        if (ench <= 0) return out;
        int mod = level + 1 + r.nextInt(ench / 4 + 1) + r.nextInt(ench / 4 + 1);
        mod = Math.max(1, Math.round(mod * (1 + (r.nextFloat() + r.nextFloat() - 1) * 0.15f)));
        while (true) {
            List<Map.Entry<Enchantment, Integer>> options = new ArrayList<>();
            for (Enchantment e : values()) {
                if (!e.canApply(item) || out.containsKey(e)) continue;
                boolean ok = true;
                for (Enchantment have : out.keySet()) if (!e.compatibleWith(have)) ok = false;
                if (!ok) continue;
                for (int l = e.maxLevel; l >= 1; l--)
                    if (mod >= e.minPower(l) && mod <= e.maxPower(l)) { options.add(Map.entry(e, l)); break; }
            }
            if (options.isEmpty()) break;
            int total = 0;
            for (var o : options) total += o.getKey().weight;
            int pick = r.nextInt(total);
            for (var o : options) {
                pick -= o.getKey().weight;
                if (pick < 0) { out.put(o.getKey(), o.getValue()); break; }
            }
            if (r.nextInt(50) > mod) break;
            mod /= 2;
        }
        return out;
    }
}
