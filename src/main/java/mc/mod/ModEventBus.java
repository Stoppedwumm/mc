package mc.mod;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * NeoForge's event bus for mods: annotated methods and lambda listeners, priorities and cancelling. A lambda added
 * without its event class is offered every event and skipped when its parameter type doesn't match.
 */
public final class ModEventBus implements IEventBus {
    private record Listener(Class<?> type, EventPriority priority, boolean receiveCanceled, Consumer<Object> consumer, Object owner,
                            java.util.Set<Class<?>> rejected) {
        Listener(Class<?> type, EventPriority priority, boolean receiveCanceled, Consumer<Object> consumer, Object owner) {
            this(type, priority, receiveCanceled, consumer, owner, java.util.concurrent.ConcurrentHashMap.newKeySet());
        }
    }

    private final String name;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public ModEventBus(String name) { this.name = name; }

    @Override
    public void register(Object target) {
        boolean statics = target instanceof Class<?>;
        Class<?> c = statics ? (Class<?>) target : target.getClass();
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Method m : k.getDeclaredMethods()) {
                SubscribeEvent ann = m.getAnnotation(SubscribeEvent.class);
                if (ann == null || Modifier.isStatic(m.getModifiers()) != statics || m.getParameterCount() != 1) continue;
                if (!Event.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
                m.setAccessible(true);
                Object receiver = statics ? null : target;
                listeners.add(new Listener(m.getParameterTypes()[0], ann.priority(), ann.receiveCanceled(), e -> invoke(m, receiver, e), target));
            }
            if (statics) break;
        }
    }

    /** Adds one static @SubscribeEvent method (used for @EventBusSubscriber classes). */
    public void registerMethod(Method m) {
        SubscribeEvent ann = m.getAnnotation(SubscribeEvent.class);
        m.setAccessible(true);
        listeners.add(new Listener(m.getParameterTypes()[0], ann.priority(), ann.receiveCanceled(), e -> invoke(m, null, e), m.getDeclaringClass()));
    }

    private static void invoke(Method m, Object receiver, Object event) {
        try {
            m.invoke(receiver, event);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t instanceof RuntimeException r) throw r;
            if (t instanceof Error err) throw err;
            throw new RuntimeException(t);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unregister(Object target) { listeners.removeIf(l -> l.owner == target); }

    @SuppressWarnings("unchecked")
    private <T extends Event> void add(EventPriority p, boolean canceled, Class<T> type, Consumer<T> c) {
        listeners.add(new Listener(type, p, canceled, (Consumer<Object>) (Consumer<?>) c, c));
    }

    @Override public <T extends Event> void addListener(Consumer<T> l) { add(EventPriority.NORMAL, false, null, l); }
    @Override public <T extends Event> void addListener(Class<T> type, Consumer<T> l) { add(EventPriority.NORMAL, false, type, l); }
    @Override public <T extends Event> void addListener(EventPriority p, Consumer<T> l) { add(p, false, null, l); }
    @Override public <T extends Event> void addListener(EventPriority p, Class<T> type, Consumer<T> l) { add(p, false, type, l); }
    @Override public <T extends Event> void addListener(EventPriority p, boolean c, Consumer<T> l) { add(p, c, null, l); }
    @Override public <T extends Event> void addListener(EventPriority p, boolean c, Class<T> type, Consumer<T> l) { add(p, c, type, l); }

    @Override
    public <T extends Event> T post(T event) {
        List<Listener> sorted = new ArrayList<>(listeners);
        sorted.sort(Comparator.comparing(Listener::priority));
        for (Listener l : sorted) {
            if (l.type != null && !l.type.isInstance(event)) continue;
            if (l.rejected.contains(event.getClass())) continue;
            if (event instanceof ICancellableEvent c && c.isCanceled() && !l.receiveCanceled) continue;
            try {
                l.consumer.accept(event);
            } catch (ClassCastException e) {
                // A lambda listener for another event type: the cast at its entry fails before any mod code runs
                StackTraceElement[] st = e.getStackTrace();
                // (the JIT may drop the stack trace of a hot exception; treat that as the entry cast too)
                boolean atEntry = l.type == null && (st.length == 0 || st[0].getClassName().equals(ModEventBus.class.getName()) || st[0].getClassName().contains("$$Lambda"));
                if (atEntry) l.rejected.add(event.getClass()); else report(event, e);
            } catch (RuntimeException | LinkageError e) {
                report(event, e);
            }
        }
        return event;
    }

    private void report(Event event, Throwable e) {
        System.err.println("[mods] " + name + " bus: listener for " + event.getClass().getSimpleName() + " failed: " + e);
        e.printStackTrace();
    }
}
