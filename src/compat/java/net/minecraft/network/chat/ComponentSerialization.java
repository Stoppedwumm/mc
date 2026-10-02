package net.minecraft.network.chat;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Codecs for text (reamc-compat: JSON text form). */
public final class ComponentSerialization {
    private ComponentSerialization() { }

    public static final Codec<Component> CODEC = Codec.PASSTHROUGH.xmap(
            d -> Component.Serializer.fromTree(d.convert(JsonOps.INSTANCE).getValue()),
            c -> new com.mojang.serialization.Dynamic<>(JsonOps.INSTANCE, Component.Serializer.toTree(c)));
    public static final Codec<Component> FLAT_CODEC = CODEC;
    public static final StreamCodec<RegistryFriendlyByteBuf, Component> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(s -> (Component) Component.Serializer.fromJson(s, null), c -> Component.Serializer.toJson(c, null)).cast();
    public static final StreamCodec<RegistryFriendlyByteBuf, java.util.Optional<Component>> OPTIONAL_STREAM_CODEC = ByteBufCodecs.optional(STREAM_CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, Component> TRUSTED_STREAM_CODEC = STREAM_CODEC;
}
