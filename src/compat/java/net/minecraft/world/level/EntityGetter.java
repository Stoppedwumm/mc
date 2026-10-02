package net.minecraft.world.level;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public interface EntityGetter {
    List<Entity> getEntities(Entity except, AABB box, Predicate<? super Entity> filter);

    <T extends Entity> List<T> getEntitiesOfClass(Class<T> type, AABB box, Predicate<? super T> filter);

    default <T extends Entity> List<T> getEntitiesOfClass(Class<T> type, AABB box) { return getEntitiesOfClass(type, box, e -> true); }

    default List<Entity> getEntities(Entity except, AABB box) { return getEntities(except, box, e -> true); }

    List<? extends Player> players();

    default Player getNearestPlayer(double x, double y, double z, double range, boolean creativeToo) {
        Player best = null;
        double bd = range < 0 ? Double.MAX_VALUE : range * range;
        for (Player p : players()) {
            if (!creativeToo && p.isCreative()) continue;
            double d = p.distanceToSqr(x, y, z);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }

    default Player getNearestPlayer(Entity e, double range) { return getNearestPlayer(e.getX(), e.getY(), e.getZ(), range, true); }

    default Player getPlayerByUUID(UUID id) {
        for (Player p : players()) if (p.getUUID().equals(id)) return p;
        return null;
    }

    default boolean hasNearbyAlivePlayer(double x, double y, double z, double range) {
        for (Player p : players()) if (p.isAlive() && p.distanceToSqr(x, y, z) < range * range) return true;
        return false;
    }
}
