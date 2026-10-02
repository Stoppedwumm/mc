package net.minecraft.world.level;

/** A world during generation (reamc-compat: features are placed into the real world). */
public interface WorldGenLevel extends ServerLevelAccessor {
    long getSeed();

    default boolean ensureCanWrite(net.minecraft.core.BlockPos pos) { return true; }

    default void setCurrentlyGenerating(java.util.function.Supplier<String> what) { }
}
