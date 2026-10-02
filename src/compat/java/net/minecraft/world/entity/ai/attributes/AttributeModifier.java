package net.minecraft.world.entity.ai.attributes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

/** A change to an attribute (reamc-compat). */
public record AttributeModifier(ResourceLocation id, double amount, Operation operation) {
    public static final MapCodec<AttributeModifier> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(AttributeModifier::id),
            Codec.DOUBLE.fieldOf("amount").forGetter(AttributeModifier::amount),
            Operation.CODEC.fieldOf("operation").forGetter(AttributeModifier::operation)
    ).apply(i, AttributeModifier::new));
    public static final Codec<AttributeModifier> CODEC = MAP_CODEC.codec();
    public static final StreamCodec<ByteBuf, AttributeModifier> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, AttributeModifier::id, ByteBufCodecs.DOUBLE, AttributeModifier::amount,
            Operation.STREAM_CODEC, AttributeModifier::operation, AttributeModifier::new);

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("id", id.toString());
        t.putDouble("amount", amount);
        t.putString("operation", operation.getSerializedName());
        return t;
    }

    public static AttributeModifier load(CompoundTag t) {
        return new AttributeModifier(ResourceLocation.parse(t.getString("id")), t.getDouble("amount"), Operation.CODEC.byName(t.getString("operation"), Operation.ADD_VALUE));
    }

    public boolean is(ResourceLocation other) { return id.equals(other); }

    public enum Operation implements StringRepresentable {
        ADD_VALUE("add_value", 0), ADD_MULTIPLIED_BASE("add_multiplied_base", 1), ADD_MULTIPLIED_TOTAL("add_multiplied_total", 2);

        public static final IntFunction<Operation> BY_ID = i -> values()[Math.floorMod(i, 3)];
        public static final StreamCodec<ByteBuf, Operation> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Operation::id);
        public static final StringRepresentable.EnumCodec<Operation> CODEC = StringRepresentable.fromEnum(Operation::values);

        private final String name;
        private final int id;

        Operation(String name, int id) { this.name = name; this.id = id; }

        public int id() { return id; }
        @Override public String getSerializedName() { return name; }
    }
}
