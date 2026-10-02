package net.minecraft.network.chat;

/** Frequently used texts (reamc-compat). */
public final class CommonComponents {
    private CommonComponents() { }

    public static final Component EMPTY = Component.empty();
    public static final Component OPTION_ON = Component.translatable("options.on");
    public static final Component OPTION_OFF = Component.translatable("options.off");
    public static final Component GUI_DONE = Component.translatable("gui.done");
    public static final Component GUI_CANCEL = Component.translatable("gui.cancel");
    public static final Component GUI_YES = Component.translatable("gui.yes");
    public static final Component GUI_NO = Component.translatable("gui.no");
    public static final Component GUI_BACK = Component.translatable("gui.back");
    public static final Component SPACE = Component.literal(" ");
    public static final Component NEW_LINE = Component.literal("\n");
    public static final Component ELLIPSIS = Component.literal("...");

    public static MutableComponent space() { return Component.literal(" "); }
}
