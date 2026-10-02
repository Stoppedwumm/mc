package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

/** The sound a note block makes over a block (reamc-compat). */
public enum NoteBlockInstrument implements StringRepresentable {
    HARP, BASEDRUM, SNARE, HAT, BASS, FLUTE, BELL, GUITAR, CHIME, XYLOPHONE, IRON_XYLOPHONE, COW_BELL, DIDGERIDOO, BIT, BANJO, PLING,
    ZOMBIE, SKELETON, CREEPER, DRAGON, WITHER_SKELETON, PIGLIN, CUSTOM_HEAD;

    @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
}
