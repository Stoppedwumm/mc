package net.minecraft.world.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Experience orbs (reamc-compat: spawned as reamc's orbs). */
public class ExperienceOrb extends Entity {
    public ExperienceOrb(mc.entity.XpOrbEntity engine) { super(engine); }

    public static void award(ServerLevel level, Vec3 pos, int amount) {
        while (amount > 0) {
            int v = amount >= 17 ? 17 : amount >= 7 ? 7 : amount >= 3 ? 3 : 1;
            amount -= v;
            mc.entity.XpOrbEntity orb = new mc.entity.XpOrbEntity(v);
            orb.setPos(pos.x, pos.y, pos.z);
            level.reamc$world().addEntity(orb);
        }
    }

    public int getValue() { return ((mc.entity.XpOrbEntity) reamc$entity).value; }
}
