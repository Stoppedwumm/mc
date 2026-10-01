package net.neoforged.neoforge.items.wrapper;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;

/** An item handler view of one side of a container (reamc-compat: all slots). */
public class SidedInvWrapper extends InvWrapper {
    private final Direction side;

    public SidedInvWrapper(WorldlyContainer inv, Direction side) {
        super(inv);
        this.side = side;
    }

    public Direction getSide() { return side; }
}
