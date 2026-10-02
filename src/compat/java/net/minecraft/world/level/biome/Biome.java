package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

/** A biome (reamc-compat: one per reamc biome, with Minecraft's climate values). */
public final class Biome {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Codec<Holder<Biome>> CODEC = (Codec) net.minecraft.resources.ResourceLocation.CODEC.xmap(
            id -> mc.mod.Bridge.dynamicRegistry((ResourceKey) Registries.BIOME).getHolder(id).orElseThrow(), h -> ((Holder<?>) h).unwrapKey().orElseThrow().location());
    public static final Codec<HolderSet<Biome>> LIST_CODEC = CODEC.listOf().xmap(HolderSet::direct, s -> s.stream().toList());

    private final float temperature, downfall;
    private final boolean precipitation;
    /** The reamc biome this is. */
    public final mc.world.gen.Biome reamc$engine;

    public Biome(mc.world.gen.Biome engine, float temperature, float downfall, boolean precipitation) {
        this.reamc$engine = engine;
        this.temperature = temperature;
        this.downfall = downfall;
        this.precipitation = precipitation;
    }

    public float getBaseTemperature() { return temperature; }
    public boolean hasPrecipitation() { return precipitation; }
    public float getDownfall() { return downfall; }
    public Precipitation getPrecipitationAt(BlockPos pos) { return !precipitation ? Precipitation.NONE : coldEnoughToSnow(pos) ? Precipitation.SNOW : Precipitation.RAIN; }
    public boolean coldEnoughToSnow(BlockPos pos) { return temperature < 0.15f; }
    public boolean warmEnoughToRain(BlockPos pos) { return !coldEnoughToSnow(pos); }
    public BiomeGenerationSettings getGenerationSettings() { return BiomeGenerationSettings.EMPTY; }
    public int getGrassColor(double x, double z) { return 0x91BD59; }
    public int getFoliageColor() { return 0x77AB2F; }

    public enum Precipitation { NONE, RAIN, SNOW }
}
