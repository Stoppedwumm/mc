package net.minecraft.world.level.block.state.properties;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;

/** The sounds and rules for a set of doors, trapdoors, buttons and plates (reamc-compat). */
public record BlockSetType(String name, boolean canOpenByHand, boolean canOpenByWindCharge, boolean canButtonBeActivatedByArrows,
                           PressurePlateSensitivity pressurePlateSensitivity, SoundType soundType, SoundEvent doorClose, SoundEvent doorOpen,
                           SoundEvent trapdoorClose, SoundEvent trapdoorOpen, SoundEvent pressurePlateClickOff, SoundEvent pressurePlateClickOn,
                           SoundEvent buttonClickOff, SoundEvent buttonClickOn) {
    public enum PressurePlateSensitivity { EVERYTHING, MOBS }

    private static BlockSetType wood(String name) {
        return new BlockSetType(name, true, true, true, PressurePlateSensitivity.EVERYTHING, SoundType.WOOD, SoundEvents.WOODEN_DOOR_CLOSE, SoundEvents.WOODEN_DOOR_OPEN,
                SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_OFF, SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON,
                SoundEvents.WOODEN_BUTTON_CLICK_OFF, SoundEvents.WOODEN_BUTTON_CLICK_ON);
    }

    public static final BlockSetType OAK = wood("oak"), SPRUCE = wood("spruce"), BIRCH = wood("birch"), ACACIA = wood("acacia"), CHERRY = wood("cherry"),
            JUNGLE = wood("jungle"), DARK_OAK = wood("dark_oak"), CRIMSON = wood("crimson"), WARPED = wood("warped"), MANGROVE = wood("mangrove"), BAMBOO = wood("bamboo");

    public static BlockSetType register(BlockSetType type) { return type; }
}
