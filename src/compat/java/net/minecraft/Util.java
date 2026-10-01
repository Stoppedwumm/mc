package net.minecraft;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Util {
    private Util() { }

    public static <T> T make(T object, Consumer<? super T> init) {
        init.accept(object);
        return object;
    }

    public static <T> T make(Supplier<T> supplier) { return supplier.get(); }

    public static long getMillis() { return System.nanoTime() / 1_000_000L; }
}
