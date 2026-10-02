package net.minecraft.world.inventory;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlagSet;

/** A kind of menu and how to build it on the client (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MenuType<T extends AbstractContainerMenu> implements net.minecraft.world.flag.FeatureElement {
    public static final MenuType<ChestMenu> GENERIC_9x1 = register("generic_9x1", ChestMenu::oneRow);
    public static final MenuType<ChestMenu> GENERIC_9x2 = register("generic_9x2", ChestMenu::twoRows);
    public static final MenuType<ChestMenu> GENERIC_9x3 = register("generic_9x3", ChestMenu::threeRows);
    public static final MenuType<ChestMenu> GENERIC_9x6 = register("generic_9x6", ChestMenu::sixRows);

    private static <T extends AbstractContainerMenu> MenuType<T> register(String id, MenuSupplier<T> f) {
        return (MenuType<T>) Registry.register((Registry) BuiltInRegistries.MENU, ResourceLocation.withDefaultNamespace(id), new MenuType<>(f, FeatureFlagSet.of()));
    }

    private final MenuSupplier<T> supplier;

    public MenuType(MenuSupplier<T> supplier, FeatureFlagSet flags) { this.supplier = supplier; }

    public MenuType(net.neoforged.neoforge.network.IContainerFactory<T> factory) { this(factory, FeatureFlagSet.of()); }

    public T create(int id, Inventory inventory) { return supplier.create(id, inventory); }

    public T create(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        if (supplier instanceof net.neoforged.neoforge.network.IContainerFactory<T> f) return f.create(id, inventory, data);
        return supplier.create(id, inventory);
    }

    @Override public FeatureFlagSet requiredFeatures() { return FeatureFlagSet.of(); }

    @FunctionalInterface
    public interface MenuSupplier<T extends AbstractContainerMenu> {
        T create(int id, Inventory inventory);
    }
}
