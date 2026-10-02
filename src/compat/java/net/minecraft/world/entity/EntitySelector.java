package net.minecraft.world.entity;

import java.util.function.Predicate;

public final class EntitySelector {
    private EntitySelector() { }

    public static final Predicate<Entity> ENTITY_STILL_ALIVE = Entity::isAlive;
    public static final Predicate<Entity> LIVING_ENTITY_STILL_ALIVE = e -> e.isAlive() && e instanceof LivingEntity;
    public static final Predicate<Entity> ENTITY_NOT_BEING_RIDDEN = e -> e.isAlive() && !e.isVehicle() && !e.isPassenger();
    public static final Predicate<Entity> CONTAINER_ENTITY_SELECTOR = e -> e instanceof net.minecraft.world.Container && e.isAlive();
    public static final Predicate<Entity> NO_CREATIVE_OR_SPECTATOR = e -> !(e instanceof net.minecraft.world.entity.player.Player p) || !p.isCreative();
    public static final Predicate<Entity> NO_SPECTATORS = e -> !e.isSpectator();
    public static final Predicate<Entity> CAN_BE_COLLIDED_WITH = NO_SPECTATORS;

    public static Predicate<Entity> withinDistance(double x, double y, double z, double range) { return e -> e.distanceToSqr(x, y, z) <= range * range; }
    public static Predicate<Entity> pushableBy(Entity e) { return o -> o != e && o.isPushable(); }
    public static Predicate<Entity> notRiding(Entity e) { return o -> o.getVehicle() != e; }
}
