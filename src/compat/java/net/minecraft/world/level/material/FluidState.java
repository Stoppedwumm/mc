package net.minecraft.world.level.material;

import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;

/** A liquid at a block: which, how much (1-8) and whether it falls (reamc-compat). */
public final class FluidState {
    private final Fluid fluid;
    private final int amount;
    private final boolean falling;

    FluidState(Fluid fluid, int amount, boolean falling) {
        this.fluid = fluid;
        this.amount = amount;
        this.falling = falling;
    }

    public Fluid getType() { return fluid; }
    public int getAmount() { return amount; }
    public float getOwnHeight() { return amount / 9f; }
    public boolean isSource() { return amount == 8 && fluid instanceof FlowingFluid f && f.getSource() == f; }
    public boolean isEmpty() { return fluid == Fluids.EMPTY; }
    public boolean isRandomlyTicking() { return false; }
    public boolean is(TagKey<Fluid> tag) { return fluid.is(tag); }
    public boolean is(Fluid f) { return fluid == f; }
    public boolean is(Holder<Fluid> f) { return fluid == f.value(); }
    public BlockState createLegacyBlock() { return fluid.reamc$block(this); }
    public boolean isSourceOfType(Fluid f) { return fluid == f && isSource(); }
    public boolean canHydrate(net.minecraft.world.level.BlockGetter getter, net.minecraft.core.BlockPos pos, BlockState state, net.minecraft.core.BlockPos statePos) { return is(net.minecraft.tags.FluidTags.WATER); }

    @Override public boolean equals(Object o) { return o instanceof FluidState s && s.fluid == fluid && s.amount == amount && s.falling == falling; }
    @Override public int hashCode() { return fluid.hashCode() * 31 + amount * 2 + (falling ? 1 : 0); }
}
