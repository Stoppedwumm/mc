package net.minecraft.world.level;

public interface ServerLevelAccessor extends LevelAccessor {
    net.minecraft.server.level.ServerLevel getLevel();

    default void addFreshEntityWithPassengers(net.minecraft.world.entity.Entity e) { addFreshEntity(e); }
}
