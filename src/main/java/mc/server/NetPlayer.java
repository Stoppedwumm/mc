package mc.server;

import mc.entity.DamageSource;
import mc.entity.Effect;
import mc.entity.Entity;
import mc.entity.Player;

/**
 * A player connected to this server. Its client simulates movement, health and inventory; the server keeps this
 * copy in the world so mobs can chase it, and forwards anything done to it (hits, knockback, effects, healing,
 * teleports) to the client, which applies it to the real player.
 */
public final class NetPlayer extends Player {
    final Session session;

    NetPlayer(Session session, String name) {
        this.session = session;
        this.name = name;
    }

    /** Position reported by the client (no teleport packet back). */
    void moveFromClient(double nx, double ny, double nz, float nyaw, float npitch) {
        prevYaw = yaw; prevPitch = pitch;
        if (vehicle != null && (vehicle.removed || vehicle.passenger != this)) vehicle = null;
        if (vehicle == null) {
            // While riding, the vehicle decides where we are
            prevX = x; prevY = y; prevZ = z;
            x = nx; y = ny; z = nz;
        }
        yaw = nyaw; pitch = npitch;
        headYaw = nyaw;
        double dx = x - prevX, dz = z - prevZ;
        float dist = (float) Math.min(1, Math.sqrt(dx * dx + dz * dz) * 4);
        prevLimbSwingAmount = limbSwingAmount;
        limbSwingAmount += (dist - limbSwingAmount) * 0.4f;
        limbSwing += limbSwingAmount;
        prevBodyYaw = bodyYaw;
        bodyYaw = nyaw;
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        if (session != null && session.player == this) session.sendTeleport(x, y, z);
    }

    @Override
    public boolean damage(DamageSource source, float amount, Entity attacker) {
        if (creative && source != DamageSource.VOID) return false;
        if (isDead()) return false;
        if (invulnerableTime > 10 && amount <= lastDamage) return false;
        lastDamage = amount;
        invulnerableTime = 20;
        hurtTime = 10;
        lastAttacker = attacker;
        session.sendHurt(source, amount, attacker);
        if (attacker instanceof mc.entity.LivingEntity le) world.commandWolves(le);
        return true;
    }

    @Override
    public void knockback(double strength, double dx, double dz) {
        session.sendKnockback(strength, dx, dz);
    }

    @Override
    public void addEffect(Effect e, int amplifier, int duration) {
        session.sendEffect(e, amplifier, duration);
    }

    @Override
    public void heal(float amount) {
        session.sendHeal(amount);
    }
}
