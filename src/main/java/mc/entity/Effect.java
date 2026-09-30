package mc.entity;

/** Status effects, with Minecraft's particle colours. */
public enum Effect {
    SPEED("Speed", 0x7cafc6, true), SLOWNESS("Slowness", 0x5a6c81, false), HASTE("Haste", 0xd9c043, true),
    STRENGTH("Strength", 0x932423, true), INSTANT_HEALTH("Instant Health", 0xf82423, true), INSTANT_DAMAGE("Instant Damage", 0x430a09, false),
    JUMP_BOOST("Jump Boost", 0x22ff4c, true), REGENERATION("Regeneration", 0xcd5cab, true), RESISTANCE("Resistance", 0x99453a, true),
    FIRE_RESISTANCE("Fire Resistance", 0xe49a3a, true), WATER_BREATHING("Water Breathing", 0x2e5299, true),
    INVISIBILITY("Invisibility", 0x7f8392, true), NIGHT_VISION("Night Vision", 0x1f1fa1, true), WEAKNESS("Weakness", 0x484d48, false),
    POISON("Poison", 0x4e9331, false), ABSORPTION("Absorption", 0x2552a5, true);

    public final String displayName;
    public final int color;
    public final boolean beneficial;

    Effect(String displayName, int color, boolean beneficial) {
        this.displayName = displayName;
        this.color = color;
        this.beneficial = beneficial;
    }

    public boolean instant() { return this == INSTANT_HEALTH || this == INSTANT_DAMAGE; }

    /** One active effect: amplifier 0 = level I. */
    public static final class Instance {
        public final Effect effect;
        public int amplifier, duration;

        public Instance(Effect effect, int amplifier, int duration) {
            this.effect = effect;
            this.amplifier = amplifier;
            this.duration = duration;
        }
    }
}
