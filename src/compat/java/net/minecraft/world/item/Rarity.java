package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

public enum Rarity implements StringRepresentable {
    COMMON(0, "common", ChatFormatting.WHITE), UNCOMMON(1, "uncommon", ChatFormatting.YELLOW), RARE(2, "rare", ChatFormatting.AQUA),
    EPIC(3, "epic", ChatFormatting.LIGHT_PURPLE);

    public static final Codec<Rarity> CODEC = StringRepresentable.fromValues(Rarity::values);
    public static final IntFunction<Rarity> BY_ID = i -> values()[Math.floorMod(i, 4)];
    public static final StreamCodec<ByteBuf, Rarity> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, r -> r.id);

    private final int id;
    private final String name;
    private final ChatFormatting color;

    Rarity(int id, String name, ChatFormatting color) { this.id = id; this.name = name; this.color = color; }

    public ChatFormatting color() { return color; }
    public UnaryOperator<Style> getStyleModifier() { return s -> s.withColor(color); }
    @Override public String getSerializedName() { return name; }
}
