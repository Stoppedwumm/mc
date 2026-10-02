package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;
import java.util.function.Predicate;

public enum EquipmentSlotGroup implements StringRepresentable {
    ANY(0, "any", s -> true), MAINHAND(1, "mainhand", EquipmentSlot.MAINHAND), OFFHAND(2, "offhand", EquipmentSlot.OFFHAND),
    HAND(3, "hand", s -> s.getType() == EquipmentSlot.Type.HAND), FEET(4, "feet", EquipmentSlot.FEET), LEGS(5, "legs", EquipmentSlot.LEGS),
    CHEST(6, "chest", EquipmentSlot.CHEST), HEAD(7, "head", EquipmentSlot.HEAD), ARMOR(8, "armor", EquipmentSlot::isArmor), BODY(9, "body", EquipmentSlot.BODY);

    public static final IntFunction<EquipmentSlotGroup> BY_ID = i -> values()[Math.floorMod(i, values().length)];
    public static final Codec<EquipmentSlotGroup> CODEC = StringRepresentable.fromEnum(EquipmentSlotGroup::values);
    public static final StreamCodec<ByteBuf, EquipmentSlotGroup> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final int id;
    private final String key;
    private final Predicate<EquipmentSlot> predicate;

    EquipmentSlotGroup(int id, String key, Predicate<EquipmentSlot> predicate) { this.id = id; this.key = key; this.predicate = predicate; }

    EquipmentSlotGroup(int id, String key, EquipmentSlot slot) { this(id, key, s -> s == slot); }

    public static EquipmentSlotGroup bySlot(EquipmentSlot s) {
        return switch (s) {
            case MAINHAND -> MAINHAND; case OFFHAND -> OFFHAND; case FEET -> FEET; case LEGS -> LEGS; case CHEST -> CHEST; case HEAD -> HEAD; case BODY -> BODY;
        };
    }

    @Override public String getSerializedName() { return key; }

    public boolean test(EquipmentSlot s) { return predicate.test(s); }
}
