package net.neoforged.fml.util.thread;

public final class SidedThreadGroups {
    private SidedThreadGroups() { }

    public static final SidedThreadGroup CLIENT = new SidedThreadGroup("CLIENT");
    public static final SidedThreadGroup SERVER = new SidedThreadGroup("SERVER");
}
