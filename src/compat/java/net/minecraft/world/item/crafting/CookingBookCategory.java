package net.minecraft.world.item.crafting;

import net.minecraft.util.StringRepresentable;

public enum CookingBookCategory implements StringRepresentable {
    FOOD("food"), BLOCKS("blocks"), MISC("misc");

    public static final StringRepresentable.EnumCodec<CookingBookCategory> CODEC = StringRepresentable.fromEnum(CookingBookCategory::values);
    public static final java.util.function.IntFunction<CookingBookCategory> BY_ID = i -> values()[Math.floorMod(i, values().length)];
    public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, CookingBookCategory> STREAM_CODEC = net.minecraft.network.codec.ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    private final String name;

    CookingBookCategory(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }
}
