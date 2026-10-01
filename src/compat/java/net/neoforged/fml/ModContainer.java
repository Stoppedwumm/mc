package net.neoforged.fml;

import net.neoforged.bus.api.IEventBus;

/** A loaded mod (reamc-compat). */
public class ModContainer {
    private final String modId, version, displayName;
    private final IEventBus bus;

    public ModContainer(String modId, String version, String displayName, IEventBus bus) {
        this.modId = modId;
        this.version = version;
        this.displayName = displayName;
        this.bus = bus;
    }

    public String getModId() { return modId; }
    public String reamc$version() { return version; }
    public String reamc$displayName() { return displayName; }
    public IEventBus getEventBus() { return bus; }
    public void registerConfig(Object type, Object spec) { }
    public void registerConfig(Object type, Object spec, String fileName) { }
    public void registerExtensionPoint(Class<?> type, Object extension) { }
}
