package net.minecraft.sounds;

import net.minecraft.resources.ResourceLocation;

public final class SoundEvents {
    private SoundEvents() { }

    private static SoundEvent of(String id) { return SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace(id)); }

    public static final SoundEvent EMPTY = of("intentionally_empty");
    public static final SoundEvent UI_BUTTON_CLICK = of("ui.button.click");
    public static final SoundEvent CHEST_OPEN = of("block.chest.open");
    public static final SoundEvent CHEST_CLOSE = of("block.chest.close");
    public static final SoundEvent PLAYER_LEVELUP = of("entity.player.levelup");
    public static final SoundEvent ITEM_PICKUP = of("entity.item.pickup");
    public static final SoundEvent GENERIC_EXPLODE = of("entity.generic.explode");
    public static final SoundEvent ANVIL_USE = of("block.anvil.use");
}
