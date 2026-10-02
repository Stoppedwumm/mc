package net.minecraft.world.level.material;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Minecraft's fluids (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Fluids {
    public static final Fluid EMPTY = register("empty", new Fluid() {
        @Override public Item getBucket() { return Items.AIR; }
    });
    public static final FlowingFluid FLOWING_WATER = register("flowing_water", new Liquid("water", false, 5));
    public static final FlowingFluid WATER = register("water", new Liquid("water", true, 5));
    public static final FlowingFluid FLOWING_LAVA = register("flowing_lava", new Liquid("lava", false, 30));
    public static final FlowingFluid LAVA = register("lava", new Liquid("lava", true, 30));

    private static <T extends Fluid> T register(String id, T fluid) {
        return (T) Registry.register((Registry) BuiltInRegistries.FLUID, ResourceLocation.withDefaultNamespace(id), fluid);
    }

    /** The fluid state of an engine block (water and lava blocks; metadata 0 is a source). */
    public static FluidState reamc$of(int blockId, int meta) {
        boolean water = blockId == mc.world.Block.WATER.id, lava = blockId == mc.world.Block.LAVA.id;
        if (!water && !lava) return EMPTY.defaultFluidState();
        FlowingFluid f = water ? WATER : LAVA;
        int level = meta & 7;
        return level == 0 ? f.getSource(false) : f.getFlowing(8 - level, (meta & 8) != 0);
    }

    static final class Liquid extends FlowingFluid {
        Liquid(String name, boolean source, int delay) { super(name, source, delay, null); }

        @Override public Fluid getSource() { return reamc$name.equals("water") ? WATER : LAVA; }
        @Override public Fluid getFlowing() { return reamc$name.equals("water") ? FLOWING_WATER : FLOWING_LAVA; }
        @Override public Item getBucket() { return reamc$name.equals("water") ? Items.WATER_BUCKET : Items.LAVA_BUCKET; }
        @Override public BlockState reamc$block(FluidState s) { return (reamc$name.equals("water") ? Blocks.WATER : Blocks.LAVA).defaultBlockState(); }
    }
}
