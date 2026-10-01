package net.neoforged.neoforge.registries;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/** Collects a mod's entries for one registry and adds them when that registry's event fires (reamc-compat). */
public class DeferredRegister<T> {
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String namespace;
    private final Map<DeferredHolder<T, ? extends T>, Supplier<? extends T>> entries = new LinkedHashMap<>();
    private boolean registered;

    protected DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        this.registryKey = registryKey;
        this.namespace = namespace;
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> key, String namespace) { return new DeferredRegister<>(key, namespace); }

    public static <T> DeferredRegister<T> create(Registry<T> registry, String namespace) { return new DeferredRegister<>(registry.key(), namespace); }

    public static Blocks createBlocks(String namespace) { return new Blocks(namespace); }

    public static Items createItems(String namespace) { return new Items(namespace); }

    protected <I extends T> DeferredHolder<T, I> createHolder(ResourceKey<? extends Registry<T>> registry, ResourceLocation id) {
        return DeferredHolder.create(ResourceKey.create(registry, id));
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> supplier) {
        return register(name, id -> supplier.get());
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Function<ResourceLocation, ? extends I> factory) {
        if (registered) throw new IllegalStateException("Cannot register new entries to DeferredRegister after it was registered");
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, name);
        DeferredHolder<T, I> holder = createHolder(registryKey, id);
        entries.put(holder, () -> factory.apply(id));
        return holder;
    }

    public void register(IEventBus bus) {
        if (registered) throw new IllegalStateException("Cannot register DeferredRegister to more than one event bus.");
        registered = true;
        bus.addListener(RegisterEvent.class, this::addEntries);
    }

    @SuppressWarnings("unchecked")
    private void addEntries(RegisterEvent event) {
        if (!event.getRegistryKey().location().equals(registryKey.location())) return;
        MappedRegistry<T> registry = (MappedRegistry<T>) event.getRegistry();
        for (var e : entries.entrySet()) {
            T value = e.getValue().get();
            registry.register(e.getKey().getId(), value);
            e.getKey().bind(value);
        }
    }

    public Collection<DeferredHolder<T, ? extends T>> getEntries() { return List.copyOf(entries.keySet()); }

    public ResourceKey<? extends Registry<T>> getRegistryKey() { return registryKey; }

    public String getNamespace() { return namespace; }

    public static class Blocks extends DeferredRegister<Block> {
        protected Blocks(String namespace) { super(Registries.BLOCK, namespace); }

        @Override
        @SuppressWarnings("unchecked")
        protected <I extends Block> DeferredHolder<Block, I> createHolder(ResourceKey<? extends Registry<Block>> registry, ResourceLocation id) {
            return (DeferredHolder<Block, I>) new DeferredBlock<I>(ResourceKey.create(registry, id));
        }

        @Override
        public <B extends Block> DeferredBlock<B> register(String name, Supplier<? extends B> supplier) {
            return (DeferredBlock<B>) super.<B>register(name, supplier);
        }

        @Override
        public <B extends Block> DeferredBlock<B> register(String name, Function<ResourceLocation, ? extends B> factory) {
            return (DeferredBlock<B>) super.<B>register(name, factory);
        }

        public <B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> factory, BlockBehaviour.Properties props) {
            return register(name, () -> factory.apply(props));
        }

        public DeferredBlock<Block> registerSimpleBlock(String name, BlockBehaviour.Properties props) { return registerBlock(name, Block::new, props); }
    }

    public static class Items extends DeferredRegister<Item> {
        protected Items(String namespace) { super(Registries.ITEM, namespace); }

        @Override
        @SuppressWarnings("unchecked")
        protected <I extends Item> DeferredHolder<Item, I> createHolder(ResourceKey<? extends Registry<Item>> registry, ResourceLocation id) {
            return (DeferredHolder<Item, I>) new DeferredItem<I>(ResourceKey.create(registry, id));
        }

        @Override
        public <I extends Item> DeferredItem<I> register(String name, Supplier<? extends I> supplier) {
            return (DeferredItem<I>) super.<I>register(name, supplier);
        }

        @Override
        public <I extends Item> DeferredItem<I> register(String name, Function<ResourceLocation, ? extends I> factory) {
            return (DeferredItem<I>) super.<I>register(name, factory);
        }

        public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> factory, Item.Properties props) {
            return register(name, () -> factory.apply(props));
        }

        public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> factory) {
            return registerItem(name, factory, new Item.Properties());
        }

        public DeferredItem<Item> registerSimpleItem(String name, Item.Properties props) { return registerItem(name, Item::new, props); }

        public DeferredItem<Item> registerSimpleItem(String name) { return registerItem(name, Item::new, new Item.Properties()); }

        public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block, Item.Properties props) {
            return register(name, () -> new BlockItem(block.get(), props));
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block) {
            return registerSimpleBlockItem(name, block, new Item.Properties());
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(DeferredHolder<Block, ?> block, Item.Properties props) {
            return registerSimpleBlockItem(block.getId().getPath(), block, props);
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(DeferredHolder<Block, ?> block) {
            return registerSimpleBlockItem(block, new Item.Properties());
        }
    }
}
