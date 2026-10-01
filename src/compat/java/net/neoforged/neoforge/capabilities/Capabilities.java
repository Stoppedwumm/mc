package net.neoforged.neoforge.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.items.IItemHandler;

public final class Capabilities {
    private Capabilities() { }

    public static final class ItemHandler {
        private ItemHandler() { }

        public static final BlockCapability<IItemHandler, Direction> BLOCK = BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("neoforge", "item_handler"), IItemHandler.class);
        public static final EntityCapability<IItemHandler, Void> ENTITY = EntityCapability.create(ResourceLocation.fromNamespaceAndPath("neoforge", "item_handler"), IItemHandler.class, Void.class);
        public static final ItemCapability<IItemHandler, Void> ITEM = ItemCapability.create(ResourceLocation.fromNamespaceAndPath("neoforge", "item_handler"), IItemHandler.class, Void.class);
    }
}
