package net.minecraft.world.level.material;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelReader;

/** A liquid that spreads (reamc-compat). */
public abstract class FlowingFluid extends Fluid {
    private final boolean source;
    private final int tickDelay;
    private final Item bucket;
    final String reamc$name;

    FlowingFluid(String name, boolean source, int tickDelay, Item bucket) {
        this.reamc$name = name;
        this.source = source;
        this.tickDelay = tickDelay;
        this.bucket = bucket;
    }

    public FluidState getSource(boolean falling) { return new FluidState(source ? this : getSource(), 8, falling); }

    public FluidState getFlowing(int amount, boolean falling) { return new FluidState(source ? getFlowing() : this, amount, falling); }

    public abstract Fluid getSource();

    public abstract Fluid getFlowing();

    @Override public int getTickDelay(LevelReader level) { return tickDelay; }

    @Override public Item getBucket() { return bucket; }
}
