package net.minecraft.core.particles;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** Item-fragment particles (reamc-compat). */
public class ItemParticleOption implements ParticleOptions {
    private final ParticleType<ItemParticleOption> type;
    private final ItemStack item;

    public ItemParticleOption(ParticleType<ItemParticleOption> type, ItemStack item) { this.type = type; this.item = item.copy(); }

    public static MapCodec<ItemParticleOption> codec(ParticleType<ItemParticleOption> type) { return MapCodec.unit(() -> new ItemParticleOption(type, ItemStack.EMPTY)); }

    public static StreamCodec<? super RegistryFriendlyByteBuf, ItemParticleOption> streamCodec(ParticleType<ItemParticleOption> type) { return StreamCodec.unit(new ItemParticleOption(type, ItemStack.EMPTY)); }

    @Override public ParticleType<ItemParticleOption> getType() { return type; }
    public ItemStack getItem() { return item; }
    @Override public String reamc$name() { return "poof"; }
}
