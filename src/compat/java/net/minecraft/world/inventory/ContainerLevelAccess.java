package net.minecraft.world.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/** The block a menu belongs to, if any (reamc-compat). */
public interface ContainerLevelAccess {
    ContainerLevelAccess NULL = new ContainerLevelAccess() {
        @Override public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> f) { return Optional.empty(); }
    };

    static ContainerLevelAccess create(Level level, BlockPos pos) {
        return new ContainerLevelAccess() {
            @Override public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> f) { return Optional.ofNullable(f.apply(level, pos)); }
        };
    }

    <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> f);

    default <T> T evaluate(BiFunction<Level, BlockPos, T> f, T fallback) { return evaluate(f).orElse(fallback); }

    default void execute(BiConsumer<Level, BlockPos> c) { evaluate((l, p) -> { c.accept(l, p); return Optional.empty(); }); }
}
