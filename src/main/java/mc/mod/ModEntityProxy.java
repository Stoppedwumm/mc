package mc.mod;

import mc.world.World;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** The engine side of an entity a mod defines: size from its type, ticking through the mod's code. */
public final class ModEntityProxy extends mc.entity.Entity {
    public final Entity mod;

    private ModEntityProxy(Entity mod, EntityType<?> type, World world) {
        this.mod = mod;
        this.world = world;
        if (type != null) {
            width = type.getWidth();
            height = type.getHeight();
        }
    }

    public static ModEntityProxy create(Entity mod, EntityType<?> type, World world) { return new ModEntityProxy(mod, type, world); }

    @Override
    public void tick() {
        prevX = x; prevY = y; prevZ = z;
        prevYaw = yaw; prevPitch = pitch;
        age++;
        if (world != null) {
            inWater = touching(mc.world.Block.WATER.id);
            inLava = touching(mc.world.Block.LAVA.id);
        }
        try {
            mod.tick();
        } catch (RuntimeException | LinkageError e) {
            System.err.println("[mods] " + mod.getClass().getName() + " failed to tick, removing it: " + e);
            remove();
        }
        if (y < -64) remove();
    }
}
