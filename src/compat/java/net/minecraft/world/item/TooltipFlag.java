package net.minecraft.world.item;

/** Whether tooltips show advanced details (reamc-compat). */
public interface TooltipFlag {
    Default NORMAL = new Default(false, false), ADVANCED = new Default(true, false);

    boolean isAdvanced();
    boolean isCreative();

    record Default(boolean advanced, boolean creative) implements TooltipFlag {
        @Override public boolean isAdvanced() { return advanced; }
        @Override public boolean isCreative() { return creative; }
        public Default asCreative() { return new Default(advanced, true); }
    }
}
