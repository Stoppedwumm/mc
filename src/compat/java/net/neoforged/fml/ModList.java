package net.neoforged.fml;

import java.util.Optional;

/** The loaded mods (reamc-compat). */
public class ModList {
    private static final ModList INSTANCE = new ModList();

    public static ModList get() { return INSTANCE; }

    public boolean isLoaded(String modId) {
        if ("minecraft".equals(modId) || "neoforge".equals(modId)) return true;
        for (var m : mc.mod.ModLoader.MODS) if (m.loaded() && m.id().equals(modId)) return true;
        return mc.mod.ModLoader.container(modId) != null;
    }

    public Optional<? extends ModContainer> getModContainerById(String modId) { return Optional.ofNullable(mc.mod.ModLoader.container(modId)); }

    public int size() { return (int) mc.mod.ModLoader.MODS.stream().filter(mc.mod.ModLoader.LoadedMod::loaded).count(); }
}
