package net.minecraft.world;

import net.minecraft.network.chat.Component;

public interface Nameable {
    Component getName();

    default Component getDisplayName() { return getName(); }

    default boolean hasCustomName() { return getCustomName() != null; }

    default Component getCustomName() { return null; }
}
