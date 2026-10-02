package net.minecraft.world.entity.ai.attributes;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** An entity attribute such as attack damage (reamc-compat). */
public class Attribute {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Codec<Holder<Attribute>> CODEC = (Codec) BuiltInRegistries.ATTRIBUTE.holderByNameCodec();
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Attribute>> STREAM_CODEC = (StreamCodec) ByteBufCodecs.holderRegistry((net.minecraft.resources.ResourceKey) net.minecraft.core.registries.Registries.ATTRIBUTE);

    private final String descriptionId;
    private final double defaultValue;
    private boolean syncable;
    private Sentiment sentiment = Sentiment.POSITIVE;

    protected Attribute(String descriptionId, double defaultValue) {
        this.descriptionId = descriptionId;
        this.defaultValue = defaultValue;
    }

    public double getDefaultValue() { return defaultValue; }
    public boolean isClientSyncable() { return syncable; }
    public Attribute setSyncable(boolean s) { syncable = s; return this; }
    public Attribute setSentiment(Sentiment s) { sentiment = s; return this; }
    public double sanitizeValue(double v) { return v; }
    public String getDescriptionId() { return descriptionId; }
    public ChatFormatting getStyle(boolean positive) { return (positive == (sentiment == Sentiment.POSITIVE)) ? ChatFormatting.BLUE : ChatFormatting.RED; }

    public enum Sentiment { POSITIVE, NEUTRAL, NEGATIVE }
}
