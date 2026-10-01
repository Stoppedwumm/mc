package net.minecraft.sounds;

import net.minecraft.resources.ResourceLocation;

/** A sound by id (reamc-compat: played as the closest of reamc's synthesised sounds). */
public class SoundEvent {
    private final ResourceLocation location;

    private SoundEvent(ResourceLocation location) { this.location = location; }

    public static SoundEvent createVariableRangeEvent(ResourceLocation location) { return new SoundEvent(location); }

    public static SoundEvent createFixedRangeEvent(ResourceLocation location, float range) { return new SoundEvent(location); }

    public ResourceLocation getLocation() { return location; }

    /** reamc's sound name for this event. */
    public String reamc$name() {
        String p = location.getPath();
        if (p.contains("door") && p.contains("open")) return "door_open";
        if (p.contains("door") || p.contains("close")) return "door_close";
        if (p.contains("click") || p.contains("button")) return "click";
        if (p.contains("explode")) return "explode";
        if (p.contains("levelup")) return "levelup";
        if (p.contains("pop") || p.contains("pickup")) return "pop";
        if (p.contains("anvil")) return "anvil";
        return "click";
    }
}
