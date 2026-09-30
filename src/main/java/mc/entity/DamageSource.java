package mc.entity;

public enum DamageSource {
    GENERIC("died"), ATTACK("was slain"), ARROW("was shot"), FALL("hit the ground too hard"), DROWN("drowned"),
    LAVA("tried to swim in lava"), FIRE("burned to death"), CACTUS("was pricked to death"), STARVE("starved to death"),
    EXPLOSION("blew up"), VOID("fell out of the world");

    public final String message;

    DamageSource(String message) {
        this.message = message;
    }
}
