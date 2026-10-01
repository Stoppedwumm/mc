package net.neoforged.fml.event.lifecycle;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.concurrent.CompletableFuture;

/** Setup events; reamc runs queued work right away (reamc-compat). */
public abstract class ParallelDispatchEvent extends Event implements IModBusEvent {
    public CompletableFuture<Void> enqueueWork(Runnable work) {
        work.run();
        return CompletableFuture.completedFuture(null);
    }
}
