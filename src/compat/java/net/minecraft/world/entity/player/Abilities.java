package net.minecraft.world.entity.player;

/** What a player may do (reamc-compat: derived from creative mode). */
public class Abilities {
    public boolean invulnerable, flying, mayfly, instabuild, mayBuild = true;
    private float flyingSpeed = 0.05f, walkingSpeed = 0.1f;

    public float getFlyingSpeed() { return flyingSpeed; }
    public void setFlyingSpeed(float f) { flyingSpeed = f; }
    public float getWalkingSpeed() { return walkingSpeed; }
    public void setWalkingSpeed(float f) { walkingSpeed = f; }
}
