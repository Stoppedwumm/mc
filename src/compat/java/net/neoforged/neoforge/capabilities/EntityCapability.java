package net.neoforged.neoforge.capabilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.IdentityHashMap;
import java.util.Map;

/** Something entities can provide (reamc-compat). */
public final class EntityCapability<T, C> {
    private final ResourceLocation name;
    final Map<EntityType<?>, ICapabilityProvider<Entity, C, T>> providers = new IdentityHashMap<>();

    private EntityCapability(ResourceLocation name) { this.name = name; }

    public static <T, C> EntityCapability<T, C> create(ResourceLocation name, Class<T> type, Class<C> context) { return new EntityCapability<>(name); }

    public static <T> EntityCapability<T, Void> createVoid(ResourceLocation name, Class<T> type) { return create(name, type, Void.class); }

    public static <T> EntityCapability<T, net.minecraft.core.Direction> createSided(ResourceLocation name, Class<T> type) { return create(name, type, net.minecraft.core.Direction.class); }

    public ResourceLocation name() { return name; }

    public T reamc$get(Entity entity, C context) {
        ICapabilityProvider<Entity, C, T> p = providers.get(entity.getType());
        return p == null ? null : p.getCapability(entity, context);
    }
}
