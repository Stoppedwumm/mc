package net.minecraft.world.item.crafting;

import net.minecraft.util.StringRepresentable;

public enum CraftingBookCategory implements StringRepresentable {
    BUILDING("building"), REDSTONE("redstone"), EQUIPMENT("equipment"), MISC("misc");

    public static final StringRepresentable.EnumCodec<CraftingBookCategory> CODEC = StringRepresentable.fromEnum(CraftingBookCategory::values);
    public static final java.util.function.IntFunction<CraftingBookCategory> BY_ID = i -> values()[Math.floorMod(i, values().length)];
    public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, CraftingBookCategory> STREAM_CODEC = net.minecraft.network.codec.ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    private final String name;

    CraftingBookCategory(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }
}
