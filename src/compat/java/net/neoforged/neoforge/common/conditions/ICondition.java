package net.neoforged.neoforge.common.conditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

/** A load condition on a data file: "neoforge:conditions" (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public interface ICondition {
    Codec<ICondition> CODEC = Codec.lazyInitialized(() -> ((Codec<MapCodec<? extends ICondition>>) (Codec) NeoForgeRegistries.CONDITION_SERIALIZERS.byNameCodec())
            .dispatch(ICondition::codec, c -> (MapCodec) c));
    Codec<List<ICondition>> LIST_CODEC = CODEC.listOf();

    boolean test(IContext context);

    MapCodec<? extends ICondition> codec();

    interface IContext {
        IContext EMPTY = new IContext() { };

        default <T> boolean isTagLoaded(TagKey<T> key) { return !mc.mod.Tags.entries(key).isEmpty(); }
    }

    /** NeoForge's built-in conditions. */
    static void reamc$registerBuiltIns() {
        reg("true", MapCodec.unit(new Const(true)));
        reg("false", MapCodec.unit(new Const(false)));
        reg("not", Codec.lazyInitialized(() -> CODEC).fieldOf("value").xmap(Not::new, Not::value));
        reg("and", LIST_CODEC.fieldOf("values").xmap(And::new, And::values));
        reg("or", LIST_CODEC.fieldOf("values").xmap(Or::new, Or::values));
        reg("mod_loaded", Codec.STRING.fieldOf("modid").xmap(ModLoaded::new, ModLoaded::modid));
        reg("item_exists", ResourceLocation.CODEC.fieldOf("item").xmap(ItemExists::new, ItemExists::item));
        reg("tag_empty", ResourceLocation.CODEC.fieldOf("tag").xmap(TagEmpty::new, TagEmpty::tag));
    }

    private static void reg(String name, MapCodec<? extends ICondition> codec) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("neoforge", name);
        if (!NeoForgeRegistries.CONDITION_SERIALIZERS.containsKey(id)) net.minecraft.core.Registry.register((net.minecraft.core.Registry) NeoForgeRegistries.CONDITION_SERIALIZERS, id, codec);
    }

    record Const(boolean value) implements ICondition {
        @Override public boolean test(IContext c) { return value; }
        @Override public MapCodec<? extends ICondition> codec() { return NeoForgeRegistries.CONDITION_SERIALIZERS.get(ResourceLocation.fromNamespaceAndPath("neoforge", value ? "true" : "false")); }
    }

    record Not(ICondition value) implements ICondition {
        @Override public boolean test(IContext c) { return !value.test(c); }
        @Override public MapCodec<? extends ICondition> codec() { return NeoForgeRegistries.CONDITION_SERIALIZERS.get(ResourceLocation.fromNamespaceAndPath("neoforge", "not")); }
    }

    record And(List<ICondition> values) implements ICondition {
        @Override public boolean test(IContext c) { return values.stream().allMatch(v -> v.test(c)); }
        @Override public MapCodec<? extends ICondition> codec() { return NeoForgeRegistries.CONDITION_SERIALIZERS.get(ResourceLocation.fromNamespaceAndPath("neoforge", "and")); }
    }

    record Or(List<ICondition> values) implements ICondition {
        @Override public boolean test(IContext c) { return values.stream().anyMatch(v -> v.test(c)); }
        @Override public MapCodec<? extends ICondition> codec() { return NeoForgeRegistries.CONDITION_SERIALIZERS.get(ResourceLocation.fromNamespaceAndPath("neoforge", "or")); }
    }

    record ModLoaded(String modid) implements ICondition {
        @Override public boolean test(IContext c) { return net.neoforged.fml.ModList.get().isLoaded(modid); }
        @Override public MapCodec<? extends ICondition> codec() { return NeoForgeRegistries.CONDITION_SERIALIZERS.get(ResourceLocation.fromNamespaceAndPath("neoforge", "mod_loaded")); }
    }

    record ItemExists(ResourceLocation item) implements ICondition {
        @Override public boolean test(IContext c) { return BuiltInRegistries.ITEM.containsKey(item); }
        @Override public MapCodec<? extends ICondition> codec() { return NeoForgeRegistries.CONDITION_SERIALIZERS.get(ResourceLocation.fromNamespaceAndPath("neoforge", "item_exists")); }
    }

    record TagEmpty(ResourceLocation tag) implements ICondition {
        @Override public boolean test(IContext c) { return mc.mod.Tags.entries(TagKey.create(net.minecraft.core.registries.Registries.ITEM, tag)).isEmpty(); }
        @Override public MapCodec<? extends ICondition> codec() { return NeoForgeRegistries.CONDITION_SERIALIZERS.get(ResourceLocation.fromNamespaceAndPath("neoforge", "tag_empty")); }
    }
}
