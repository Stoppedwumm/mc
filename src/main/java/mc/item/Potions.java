package mc.item;

import mc.entity.Effect;

/**
 * Potions are one item whose stack damage encodes the type: bits 0-4 the base potion, bit 5 extended
 * duration (redstone), bit 6 level II (glowstone). Brewing follows Minecraft's recipes.
 */
public final class Potions {
    public enum Type {
        //                name                effect                   ticks   long    strong
        WATER("Water Bottle", null, 0, 0, 0),
        AWKWARD("Awkward Potion", null, 0, 0, 0),
        MUNDANE("Mundane Potion", null, 0, 0, 0),
        THICK("Thick Potion", null, 0, 0, 0),
        SWIFTNESS("Swiftness", Effect.SPEED, 3600, 9600, 1800),
        SLOWNESS("Slowness", Effect.SLOWNESS, 1800, 4800, 400),
        STRENGTH("Strength", Effect.STRENGTH, 3600, 9600, 1800),
        WEAKNESS("Weakness", Effect.WEAKNESS, 1800, 4800, 0),
        HEALING("Healing", Effect.INSTANT_HEALTH, 1, 0, 1),
        HARMING("Harming", Effect.INSTANT_DAMAGE, 1, 0, 1),
        POISON("Poison", Effect.POISON, 900, 1800, 432),
        REGENERATION("Regeneration", Effect.REGENERATION, 900, 1800, 450),
        FIRE_RESISTANCE("Fire Resistance", Effect.FIRE_RESISTANCE, 3600, 9600, 0),
        NIGHT_VISION("Night Vision", Effect.NIGHT_VISION, 3600, 9600, 0),
        INVISIBILITY("Invisibility", Effect.INVISIBILITY, 3600, 9600, 0),
        WATER_BREATHING("Water Breathing", Effect.WATER_BREATHING, 3600, 9600, 0),
        LEAPING("Leaping", Effect.JUMP_BOOST, 3600, 9600, 1800);

        public final String name;
        public final Effect effect;
        public final int duration, longDuration, strongDuration;

        Type(String name, Effect effect, int duration, int longDuration, int strongDuration) {
            this.name = name;
            this.effect = effect;
            this.duration = duration;
            this.longDuration = longDuration;
            this.strongDuration = strongDuration;
        }
    }

    public static final int LONG = 32, STRONG = 64;

    private Potions() { }

    public static Type type(int meta) {
        int t = meta & 31;
        return t < Type.values().length ? Type.values()[t] : Type.WATER;
    }

    public static int meta(Type t) { return t.ordinal(); }

    public static int color(int meta) {
        Type t = type(meta);
        if (t.effect == null) return 0x385dc6;
        return t.effect.color;
    }

    /** Effect amplifier and duration of a potion (duration 0 for base potions). */
    public static int amplifier(int meta) { return (meta & STRONG) != 0 ? 1 : 0; }

    public static int duration(int meta) {
        Type t = type(meta);
        if ((meta & LONG) != 0) return t.longDuration;
        if ((meta & STRONG) != 0) return t.strongDuration;
        return t.duration;
    }

    public static String name(Item item, int meta) {
        Type t = type(meta);
        boolean splash = item == Item.SPLASH_POTION;
        if (t.effect == null) return (splash ? "Splash " : "") + t.name;
        return (splash ? "Splash Potion of " : "Potion of ") + t.name;
    }

    public static String describe(int meta) {
        Type t = type(meta);
        if (t.effect == null) return "No Effects";
        String lv = amplifier(meta) > 0 ? " II" : "";
        if (t.effect.instant()) return t.effect.displayName + lv;
        int s = duration(meta) / 20;
        return t.effect.displayName + lv + String.format(" (%d:%02d)", s / 60, s % 60);
    }

    /** Result of brewing an ingredient into a potion, or -1 if nothing happens. */
    public static int brew(int meta, Item ing) {
        Type t = type(meta);
        boolean isLong = (meta & LONG) != 0, strong = (meta & STRONG) != 0;
        if (t == Type.WATER) {
            if (ing == Item.NETHER_WART) return meta(Type.AWKWARD);
            if (ing == Item.FERMENTED_SPIDER_EYE) return meta(Type.WEAKNESS);
            if (ing == Item.GLOWSTONE_DUST) return meta(Type.THICK);
            if (ing == Item.REDSTONE || ing == Item.SUGAR || ing == Item.SPIDER_EYE || ing == Item.GHAST_TEAR || ing == Item.BLAZE_POWDER
                    || ing == Item.MAGMA_CREAM || ing == Item.GLISTERING_MELON) return meta(Type.MUNDANE);
            return -1;
        }
        if (t == Type.AWKWARD) {
            Type out = null;
            if (ing == Item.SUGAR) out = Type.SWIFTNESS;
            else if (ing == Item.GLISTERING_MELON) out = Type.HEALING;
            else if (ing == Item.SPIDER_EYE) out = Type.POISON;
            else if (ing == Item.GHAST_TEAR) out = Type.REGENERATION;
            else if (ing == Item.BLAZE_POWDER) out = Type.STRENGTH;
            else if (ing == Item.MAGMA_CREAM) out = Type.FIRE_RESISTANCE;
            else if (ing == Item.GOLDEN_CARROT) out = Type.NIGHT_VISION;
            else if (ing == Item.SLIMEBALL) out = Type.LEAPING;
            else if (ing == Item.INK_SAC) out = Type.WATER_BREATHING;
            return out == null ? -1 : meta(out);
        }
        if (t.effect == null) return -1;
        if (ing == Item.REDSTONE && t.longDuration > 0 && !isLong) return (meta & 31) | LONG;
        if (ing == Item.GLOWSTONE_DUST && t.strongDuration > 0 && !strong) return (meta & 31) | STRONG;
        if (ing == Item.FERMENTED_SPIDER_EYE) {
            int keep = meta & (LONG | STRONG);
            return switch (t) {
                case SWIFTNESS, LEAPING -> meta(Type.SLOWNESS) | (keep & LONG);
                case HEALING, POISON -> meta(Type.HARMING) | (keep & STRONG);
                case NIGHT_VISION -> meta(Type.INVISIBILITY) | (keep & LONG);
                default -> -1;
            };
        }
        return -1;
    }

    /** Applies a potion's effect to an entity, scaled for splashes (1 = direct). */
    public static void apply(mc.entity.LivingEntity e, int meta, double scale) {
        Type t = type(meta);
        if (t.effect == null || scale <= 0) return;
        int amp = amplifier(meta);
        if (t.effect.instant()) {
            // Healing hurts the undead and harming heals them
            boolean undead = e instanceof mc.entity.Mob m && m.type.isUndead();
            if ((t.effect == Effect.INSTANT_HEALTH) != undead) e.heal((float) ((4 << amp) * scale));
            else e.damage(mc.entity.DamageSource.MAGIC, (float) ((6 << amp) * scale), null);
            return;
        }
        int d = (int) (duration(meta) * scale);
        if (d > 20) e.addEffect(t.effect, amp, d);
    }
}
