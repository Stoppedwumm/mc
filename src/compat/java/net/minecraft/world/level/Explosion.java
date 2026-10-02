package net.minecraft.world.level;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** An explosion as blocks see it (reamc-compat). */
public class Explosion {
    public enum BlockInteraction { KEEP, DESTROY, DESTROY_WITH_DECAY, TRIGGER_BLOCK }

    private final Level level;
    private final Entity source;
    private final double x, y, z;
    private final float radius;
    private final boolean fire;
    private final BlockInteraction interaction;

    public Explosion(Level level, Entity source, double x, double y, double z, float radius, boolean fire, BlockInteraction interaction) {
        this.level = level;
        this.source = source;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.fire = fire;
        this.interaction = interaction;
    }

    public Vec3 center() { return new Vec3(x, y, z); }
    public float radius() { return radius; }
    public Entity getDirectSourceEntity() { return source; }
    public LivingEntity getIndirectSourceEntity() { return source instanceof LivingEntity l ? l : null; }
    public BlockInteraction getBlockInteraction() { return interaction; }
    public boolean canTriggerBlocks() { return interaction == BlockInteraction.TRIGGER_BLOCK; }
    public boolean reamc$fire() { return fire; }
    public Level reamc$level() { return level; }
}
