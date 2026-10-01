package net.neoforged.neoforge.registries;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.function.Consumer;

/** Fired once per registry while mods load; listeners add their entries (reamc-compat). */
public class RegisterEvent extends Event implements IModBusEvent {
    private final Registry<?> registry;

    public RegisterEvent(Registry<?> registry) { this.registry = registry; }

    public ResourceKey<? extends Registry<?>> getRegistryKey() { return registry.key(); }

    public Registry<?> getRegistry() { return registry; }

    @SuppressWarnings("unchecked")
    public <T> void register(ResourceKey<? extends Registry<T>> key, Consumer<RegisterHelper<T>> consumer) {
        if (!key.location().equals(registry.key().location())) return;
        MappedRegistry<T> r = (MappedRegistry<T>) registry;
        consumer.accept(r::register);
    }

    @SuppressWarnings("unchecked")
    public <T> void register(ResourceKey<? extends Registry<T>> key, ResourceLocation name, java.util.function.Supplier<T> value) {
        if (key.location().equals(registry.key().location())) ((MappedRegistry<T>) registry).register(name, value.get());
    }

    @FunctionalInterface
    public interface RegisterHelper<T> {
        void register(ResourceLocation name, T value);

        default void register(ResourceKey<T> key, T value) { register(key.location(), value); }
    }
}
