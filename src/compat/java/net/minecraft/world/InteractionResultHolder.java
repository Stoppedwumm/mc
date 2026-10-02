package net.minecraft.world;

/** A result plus the item stack after the action (reamc-compat). */
public class InteractionResultHolder<T> {
    private final InteractionResult result;
    private final T object;

    public InteractionResultHolder(InteractionResult result, T object) { this.result = result; this.object = object; }

    public InteractionResult getResult() { return result; }
    public T getObject() { return object; }

    public static <T> InteractionResultHolder<T> success(T t) { return new InteractionResultHolder<>(InteractionResult.SUCCESS, t); }
    public static <T> InteractionResultHolder<T> consume(T t) { return new InteractionResultHolder<>(InteractionResult.CONSUME, t); }
    public static <T> InteractionResultHolder<T> pass(T t) { return new InteractionResultHolder<>(InteractionResult.PASS, t); }
    public static <T> InteractionResultHolder<T> fail(T t) { return new InteractionResultHolder<>(InteractionResult.FAIL, t); }
    public static <T> InteractionResultHolder<T> sidedSuccess(T t, boolean client) { return client ? success(t) : consume(t); }
}
