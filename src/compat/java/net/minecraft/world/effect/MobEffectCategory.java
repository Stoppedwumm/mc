package net.minecraft.world.effect;

import net.minecraft.ChatFormatting;

public enum MobEffectCategory {
    BENEFICIAL(ChatFormatting.BLUE), HARMFUL(ChatFormatting.RED), NEUTRAL(ChatFormatting.BLUE);

    private final ChatFormatting format;

    MobEffectCategory(ChatFormatting format) { this.format = format; }

    public ChatFormatting getTooltipFormatting() { return format; }
}
