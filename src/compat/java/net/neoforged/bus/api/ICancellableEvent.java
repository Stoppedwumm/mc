package net.neoforged.bus.api;

public interface ICancellableEvent {
    default void setCanceled(boolean canceled) { ((Event) this).reamc$canceled = canceled; }

    default boolean isCanceled() { return ((Event) this).reamc$canceled; }
}
