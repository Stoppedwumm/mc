package net.minecraft.world.level.block.state.properties;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/** A kind of wood: its sounds and the sets of signs, gates and doors made from it (reamc-compat). */
public record WoodType(String name, BlockSetType setType, SoundType soundType, SoundType hangingSignSoundType, SoundEvent fenceGateClose, SoundEvent fenceGateOpen) {
    private static final Map<String, WoodType> TYPES = new LinkedHashMap<>();

    public WoodType(String name, BlockSetType setType) {
        this(name, setType, SoundType.WOOD, SoundType.HANGING_SIGN, SoundEvents.FENCE_GATE_CLOSE, SoundEvents.FENCE_GATE_OPEN);
    }

    public static final WoodType OAK = register(new WoodType("oak", BlockSetType.OAK));
    public static final WoodType SPRUCE = register(new WoodType("spruce", BlockSetType.SPRUCE));
    public static final WoodType BIRCH = register(new WoodType("birch", BlockSetType.BIRCH));
    public static final WoodType ACACIA = register(new WoodType("acacia", BlockSetType.ACACIA));
    public static final WoodType CHERRY = register(new WoodType("cherry", BlockSetType.CHERRY));
    public static final WoodType JUNGLE = register(new WoodType("jungle", BlockSetType.JUNGLE));
    public static final WoodType DARK_OAK = register(new WoodType("dark_oak", BlockSetType.DARK_OAK));
    public static final WoodType CRIMSON = register(new WoodType("crimson", BlockSetType.CRIMSON));
    public static final WoodType WARPED = register(new WoodType("warped", BlockSetType.WARPED));
    public static final WoodType MANGROVE = register(new WoodType("mangrove", BlockSetType.MANGROVE));
    public static final WoodType BAMBOO = register(new WoodType("bamboo", BlockSetType.BAMBOO));

    public static WoodType register(WoodType type) {
        TYPES.put(type.name(), type);
        return type;
    }

    public static Stream<WoodType> values() { return TYPES.values().stream(); }
}
