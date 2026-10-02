package net.minecraft.world.level.material;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** A liquid kind (reamc-compat: water and lava, which reamc simulates itself). */
public abstract class Fluid {
    private final FluidState defaultState;

    protected Fluid() { defaultState = new FluidState(this, 0, false); }

    public FluidState defaultFluidState() { return defaultState; }

    public abstract Item getBucket();

    public boolean isSame(Fluid other) { return other == this; }

    public boolean is(TagKey<Fluid> tag) {
        var id = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(this);
        return id != null && mc.mod.Tags.has(tag, id);
    }

    public int getTickDelay(LevelReader level) { return 5; }

    public BlockState reamc$block(FluidState s) { return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(); }
}
