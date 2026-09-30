package mc.entity;

public enum DamageSource {
    GENERIC("died", false), ATTACK("was slain", false), ARROW("was shot", false), FALL("hit the ground too hard", true),
    DROWN("drowned", true), LAVA("tried to swim in lava", false), FIRE("burned to death", true), CACTUS("was pricked to death", false),
    STARVE("starved to death", true), EXPLOSION("blew up", false), VOID("fell out of the world", true),
    MAGIC("was killed by magic", true);

    public final String message;
    /** Damage that armor does not reduce. */
    public final boolean bypassesArmor;

    DamageSource(String message, boolean bypassesArmor) {
        this.message = message;
        this.bypassesArmor = bypassesArmor;
    }
}
