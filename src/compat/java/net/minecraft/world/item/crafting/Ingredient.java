package net.minecraft.world.item.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** What a recipe slot accepts: items, tags or a mod's custom ingredient (reamc-compat). */
public final class Ingredient implements Predicate<ItemStack> {
    public static final Ingredient EMPTY = new Ingredient(List.of(), null);

    /** {"item": id} | {"tag": id} | [those] | {"type": custom type, ...}. */
    public static final Codec<Ingredient> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<Ingredient, T>> decode(DynamicOps<T> ops, T input) {
            try {
                return DataResult.success(Pair.of(fromJson(new Dynamic<>(ops, input).convert(JsonOps.INSTANCE).getValue()), ops.empty()));
            } catch (RuntimeException e) {
                return DataResult.error(() -> "Bad ingredient: " + e.getMessage());
            }
        }

        @Override
        public <T> DataResult<T> encode(Ingredient i, DynamicOps<T> ops, T prefix) {
            return DataResult.success(new Dynamic<>(JsonOps.INSTANCE, i.toJson()).convert(ops).getValue());
        }
    };
    public static final Codec<Ingredient> CODEC_NONEMPTY = CODEC.validate(i -> i.isEmpty() ? DataResult.error(() -> "Item array cannot be empty, at least one item must be defined") : DataResult.success(i));
    public static final MapCodec<Ingredient> MAP_CODEC_NONEMPTY = MapCodec.assumeMapUnsafe(CODEC_NONEMPTY);
    public static final Codec<List<Ingredient>> LIST_CODEC = CODEC.listOf();
    public static final Codec<List<Ingredient>> LIST_CODEC_NONEMPTY = CODEC_NONEMPTY.listOf();
    public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> CONTENTS_STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(s -> fromJson(com.google.gson.JsonParser.parseString(s)), i -> i.toJson().toString()).cast();

    private final List<Value> values;
    private final ICustomIngredient custom;
    private ItemStack[] stacks;

    private Ingredient(List<Value> values, ICustomIngredient custom) {
        this.values = values;
        this.custom = custom;
    }

    public Ingredient(ICustomIngredient custom) { this(List.of(), custom); }

    public static Ingredient of() { return EMPTY; }

    public static Ingredient of(ItemLike... items) {
        List<Value> l = new ArrayList<>();
        for (ItemLike i : items) l.add(new ItemValue(new ItemStack(i.asItem())));
        return l.isEmpty() ? EMPTY : new Ingredient(l, null);
    }

    public static Ingredient of(ItemStack... stacks) {
        List<Value> l = new ArrayList<>();
        for (ItemStack s : stacks) if (!s.isEmpty()) l.add(new ItemValue(s.copy()));
        return l.isEmpty() ? EMPTY : new Ingredient(l, null);
    }

    public static Ingredient of(Stream<ItemStack> stacks) { return of(stacks.toArray(ItemStack[]::new)); }

    public static Ingredient of(TagKey<Item> tag) { return new Ingredient(List.of(new TagValue(tag)), null); }

    public boolean isCustom() { return custom != null; }

    public ICustomIngredient getCustomIngredient() { return custom; }

    public boolean isSimple() { return custom == null || custom.isSimple(); }

    /** Every stack it accepts (tags resolved). */
    public ItemStack[] getItems() {
        if (stacks == null) {
            if (custom != null) stacks = custom.getItems().toArray(ItemStack[]::new);
            else {
                List<ItemStack> l = new ArrayList<>();
                for (Value v : values) for (ItemStack s : v.getItems()) if (!s.isEmpty()) l.add(s);
                stacks = l.toArray(ItemStack[]::new);
            }
        }
        return stacks;
    }

    public boolean hasNoItems() { return getItems().length == 0; }

    public boolean isEmpty() { return custom == null && values.isEmpty(); }

    @Override
    public boolean test(ItemStack s) {
        if (s == null) return false;
        if (custom != null) return custom.test(s);
        if (isEmpty()) return s.isEmpty();
        for (Value v : values) if (v.test(s)) return true;
        return false;
    }

    public it.unimi.dsi.fastutil.ints.IntList getStackingIds() {
        it.unimi.dsi.fastutil.ints.IntArrayList l = new it.unimi.dsi.fastutil.ints.IntArrayList();
        for (ItemStack s : getItems()) l.add(BuiltInRegistries.ITEM.getId(s.getItem()));
        return l;
    }

    /** The engine items it accepts (for reamc's crafting). */
    public List<mc.item.Item> reamc$engineItems() {
        LinkedHashSet<mc.item.Item> out = new LinkedHashSet<>();
        for (ItemStack s : getItems()) if (s.reamc$handle() != null) out.add(s.reamc$handle().item);
        return new ArrayList<>(out);
    }

    // ------------------------------------------------------------------ JSON

    private JsonElement toJson() {
        if (custom != null) {
            ResourceLocation type = net.neoforged.neoforge.registries.NeoForgeRegistries.INGREDIENT_TYPES.getKey(custom.getType());
            @SuppressWarnings({"unchecked", "rawtypes"})
            JsonElement j = (JsonElement) ((MapCodec) custom.getType().codec()).codec().encodeStart(JsonOps.INSTANCE, custom).getOrThrow();
            JsonObject o = j.isJsonObject() ? j.getAsJsonObject() : new JsonObject();
            o.addProperty("type", String.valueOf(type));
            return o;
        }
        if (values.size() == 1) return values.get(0).toJson();
        JsonArray a = new JsonArray();
        for (Value v : values) a.add(v.toJson());
        return a;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Ingredient fromJson(JsonElement j) {
        if (j.isJsonArray()) {
            List<Value> l = new ArrayList<>();
            for (JsonElement e : j.getAsJsonArray()) {
                Ingredient i = fromJson(e);
                if (i.custom != null) return i;
                l.addAll(i.values);
            }
            return l.isEmpty() ? EMPTY : new Ingredient(l, null);
        }
        JsonObject o = j.getAsJsonObject();
        if (o.has("type")) {
            ResourceLocation type = ResourceLocation.parse(o.get("type").getAsString());
            net.neoforged.neoforge.common.crafting.IngredientType<?> t = net.neoforged.neoforge.registries.NeoForgeRegistries.INGREDIENT_TYPES.get(type);
            if (t == null) throw new IllegalArgumentException("Unknown ingredient type " + type);
            ICustomIngredient c = (ICustomIngredient) ((MapCodec) t.codec()).codec().parse(JsonOps.INSTANCE, o).getOrThrow();
            return new Ingredient(c);
        }
        if (o.has("tag")) return of(ItemTags.create(ResourceLocation.parse(o.get("tag").getAsString())));
        if (o.has("item")) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(o.get("item").getAsString()));
            return new Ingredient(List.of(new ItemValue(new ItemStack(item))), null);
        }
        throw new IllegalArgumentException("Ingredient needs an item, a tag or a type: " + o);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Ingredient i && (custom != null ? custom.equals(i.custom) : values.equals(i.values));
    }

    @Override public int hashCode() { return custom != null ? custom.hashCode() : values.hashCode(); }

    @Override public String toString() { return "Ingredient" + toJson(); }

    // ------------------------------------------------------------------ values

    public interface Value {
        java.util.Collection<ItemStack> getItems();

        boolean test(ItemStack s);

        JsonElement toJson();
    }

    public record ItemValue(ItemStack item) implements Value {
        @Override public java.util.Collection<ItemStack> getItems() { return List.of(item); }
        @Override public boolean test(ItemStack s) { return !s.isEmpty() && s.getItem() == item.getItem(); }

        @Override
        public JsonElement toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("item", String.valueOf(BuiltInRegistries.ITEM.getKey(item.getItem())));
            return o;
        }

        @Override public boolean equals(Object o) { return o instanceof ItemValue v && v.item.getItem() == item.getItem(); }
        @Override public int hashCode() { return item.getItem().hashCode(); }
    }

    public record TagValue(TagKey<Item> tag) implements Value {
        @Override
        public java.util.Collection<ItemStack> getItems() {
            List<ItemStack> l = new ArrayList<>();
            for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                ItemStack s = new ItemStack(h);
                if (!s.isEmpty()) l.add(s);
            }
            return l;
        }

        @Override public boolean test(ItemStack s) { return s.is(tag); }

        @Override
        public JsonElement toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("tag", tag.location().toString());
            return o;
        }
    }

    /** All non-empty ingredients of a recipe in a list (reamc-compat helper). */
    public static NonNullList<Ingredient> reamc$list(List<Ingredient> l) {
        NonNullList<Ingredient> out = NonNullList.create();
        out.addAll(l);
        return out;
    }
}
