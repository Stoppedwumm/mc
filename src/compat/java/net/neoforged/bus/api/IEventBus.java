package net.neoforged.bus.api;

import java.util.function.Consumer;

/** Delivers events to listeners (reamc-compat; implemented by mc.mod.ModEventBus). */
public interface IEventBus {
    void register(Object target);

    void unregister(Object target);

    <T extends Event> void addListener(Consumer<T> listener);

    <T extends Event> void addListener(Class<T> eventType, Consumer<T> listener);

    <T extends Event> void addListener(EventPriority priority, Consumer<T> listener);

    <T extends Event> void addListener(EventPriority priority, Class<T> eventType, Consumer<T> listener);

    <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Consumer<T> listener);

    <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Class<T> eventType, Consumer<T> listener);

    <T extends Event> T post(T event);

    default void start() { }
}
