package net.minecraft.sounds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** A sound by id (reamc-compat: played as the closest of reamc's synthesised sounds). */
public class SoundEvent {
    public static final Codec<SoundEvent> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("sound_id").forGetter(SoundEvent::getLocation),
            Codec.FLOAT.lenientOptionalFieldOf("range").forGetter(e -> Optional.ofNullable(e.fixedRange))
    ).apply(i, (id, range) -> range.map(r -> createFixedRangeEvent(id, r)).orElseGet(() -> createVariableRangeEvent(id))));
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Codec<Holder<SoundEvent>> CODEC = (Codec) BuiltInRegistries.SOUND_EVENT.holderByNameCodec();
    public static final StreamCodec<ByteBuf, SoundEvent> DIRECT_STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SoundEvent::getLocation,
            ByteBufCodecs.FLOAT.apply(ByteBufCodecs::optional), e -> Optional.ofNullable(e.fixedRange),
            (id, range) -> range.map(r -> createFixedRangeEvent(id, r)).orElseGet(() -> createVariableRangeEvent(id)));
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<SoundEvent>> STREAM_CODEC = (StreamCodec) ByteBufCodecs.holder((net.minecraft.resources.ResourceKey) net.minecraft.core.registries.Registries.SOUND_EVENT, DIRECT_STREAM_CODEC);

    private final ResourceLocation location;
    private final Float fixedRange;

    private SoundEvent(ResourceLocation location, Float fixedRange) {
        this.location = location;
        this.fixedRange = fixedRange;
    }

    public static SoundEvent createVariableRangeEvent(ResourceLocation location) { return new SoundEvent(location, null); }

    public static SoundEvent createFixedRangeEvent(ResourceLocation location, float range) { return new SoundEvent(location, range); }

    public ResourceLocation getLocation() { return location; }

    public float getRange(float volume) { return fixedRange != null ? fixedRange : volume > 1 ? 16 * volume : 16; }

    /** reamc's sound name for this event ("" = none fits). */
    public String reamc$name() {
        String p = location.getPath();
        if (p.contains("burp")) return "burp";
        if (p.contains("eat")) return "eat";
        if (p.contains("drink")) return "drink";
        if (p.contains("shear")) return "shear";
        if (p.contains("extinguish") || p.contains("fizz")) return "fizz";
        if (p.contains("flintandsteel") || p.contains("firecharge") || p.contains("ignite")) return "ignite";
        if (p.contains("bucket")) return "bucket";
        if (p.contains("door") && p.contains("open")) return "door_open";
        if (p.contains("door") || p.contains("chest.close") || p.contains("barrel.close")) return "door_close";
        if (p.contains("chest.open") || p.contains("barrel.open")) return "door_open";
        if (p.contains("glass") && p.contains("break")) return "glass";
        if (p.contains("click") || p.contains("button") || p.contains("lever")) return "click";
        if (p.contains("explode")) return "explode";
        if (p.contains("levelup")) return "levelup";
        if (p.contains("experience_orb") || p.contains("orb")) return "orb";
        if (p.contains("pop") || p.contains("pickup")) return "pop";
        if (p.contains("anvil")) return "anvil";
        if (p.contains("splash")) return "splash";
        if (p.contains("brewing")) return "brew";
        if (p.contains("armor.equip")) return "armor";
        if (p.contains("piston.extend")) return "piston_out";
        if (p.contains("piston.contract")) return "piston_in";
        if (p.endsWith(".hurt")) return "hurt";
        return "";
    }

    @Override public String toString() { return "SoundEvent[" + location + "]"; }
}
