package mc.mod;

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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loot tables as data: Minecraft's entry, condition and function types are evaluated here from their JSON; types
 * mods register are decoded with their codecs. NeoForge global loot modifiers run on every roll.
 */
public final class Loot {
    private Loot() { }

    private static final Map<ResourceLocation, JsonObject> TABLES = new LinkedHashMap<>();
    private static final List<IGlobalLootModifier> MODIFIERS = new ArrayList<>();

    // ------------------------------------------------------------------ codecs mods use for conditions and functions

    public static final Codec<LootItemCondition> CONDITION_CODEC = jsonCodec(Loot::condition);
    public static final Codec<LootItemFunction> FUNCTION_CODEC = jsonCodec(Loot::function);

    /** A codec that reads JSON into a value (and cannot write it back: loot is read-only data). */
    private static <T> Codec<T> jsonCodec(java.util.function.Function<JsonElement, T> reader) {
        return new Codec<>() {
            @Override
            public <U> DataResult<Pair<T, U>> decode(DynamicOps<U> ops, U input) {
                try {
                    return DataResult.success(Pair.of(reader.apply(new Dynamic<>(ops, input).convert(JsonOps.INSTANCE).getValue()), ops.empty()));
                } catch (RuntimeException e) {
                    return DataResult.error(() -> "Bad loot data: " + e.getMessage());
                }
            }

            @Override
            public <U> DataResult<U> encode(T value, DynamicOps<U> ops, U prefix) { return DataResult.error(() -> "Loot data can't be written"); }
        };
    }

    // ------------------------------------------------------------------ loading

    public static synchronized void addTable(ResourceLocation id, JsonObject table) { TABLES.put(id, table); }

    public static boolean hasTable(ResourceLocation id) { return TABLES.containsKey(id); }

    /** Reads NeoForge's global loot modifier list and the modifiers it names. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized void loadModifiers(Map<String, JsonObject> files) {
        MODIFIERS.clear();
        JsonObject list = files.get("neoforge:global_loot_modifiers");
        if (list == null) return;
        for (JsonElement e : list.getAsJsonArray("entries")) {
            String id = e.getAsString();
            JsonObject m = files.get(id.contains(":") ? id : "minecraft:" + id);
            if (m == null) continue;
            try {
                ResourceLocation type = ResourceLocation.parse(m.get("type").getAsString());
                MapCodec<?> codec = (MapCodec<?>) net.neoforged.neoforge.registries.NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.get(type);
                if (codec == null) {
                    if (type.equals(ResourceLocation.parse("neoforge:add_table"))) codec = net.neoforged.neoforge.common.loot.AddTableLootModifier.CODEC;
                    else { System.err.println("[mods] Unknown loot modifier type " + type + " in " + id); continue; }
                }
                MODIFIERS.add((IGlobalLootModifier) ((MapCodec) codec).codec().parse(JsonOps.INSTANCE, m).getOrThrow());
            } catch (RuntimeException ex) {
                System.err.println("[mods] Loot modifier " + id + " failed to load: " + ex);
            }
        }
    }

    public static int modifierCount() { return MODIFIERS.size(); }

    // ------------------------------------------------------------------ rolling

    /** Rolls a table and applies the global loot modifiers. */
    public static List<ItemStack> roll(ResourceLocation id, LootContext ctx) {
        ObjectArrayList<ItemStack> out = new ObjectArrayList<>();
        JsonObject t = TABLES.get(id);
        if (t != null) rollTable(t, ctx, out, 0);
        return applyModifiers(id, out, ctx);
    }

    /** A block's drops from its loot table; Minecraft's own blocks (which have no table here) drop themselves. */
    public static List<ItemStack> blockDrops(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        var key = state.getBlock().getLootTable();
        params.withParameter(LootContextParams.BLOCK_STATE, state);
        var lp = params.create(null);
        LootContext ctx = new LootContext.Builder(lp).create(java.util.Optional.of(key.location()));
        if (!hasTable(key.location())) {
            ObjectArrayList<ItemStack> self = new ObjectArrayList<>();
            if (!state.getBlock().reamc$isModded() && state.getBlock().asItem() != net.minecraft.world.item.Items.AIR) self.add(new ItemStack(state.getBlock()));
            return applyModifiers(key.location(), self, ctx);
        }
        return roll(key.location(), ctx);
    }

    /** Runs the global loot modifiers over drops reamc computed itself (for Minecraft's own tables). */
    public static ObjectArrayList<ItemStack> applyModifiers(ResourceLocation table, ObjectArrayList<ItemStack> loot, LootContext ctx) {
        LootContext c = ctx.getQueriedLootTableId() != null ? ctx : new LootContext.Builder(ctx.reamc$params()).withOptionalRandomSource(ctx.getRandom()).create(java.util.Optional.of(table));
        for (IGlobalLootModifier m : MODIFIERS) {
            try {
                loot = m.apply(loot, c);
            } catch (RuntimeException e) {
                System.err.println("[mods] Loot modifier " + m.getClass().getSimpleName() + " failed: " + e);
            }
        }
        return loot;
    }

    private static void rollTable(JsonObject t, LootContext ctx, List<ItemStack> out, int depth) {
        if (depth > 8) return;
        List<ItemStack> local = new ArrayList<>();
        if (t.has("pools")) for (JsonElement p : t.getAsJsonArray("pools")) rollPool(p.getAsJsonObject(), ctx, local, depth);
        applyFunctions(t, local, ctx);
        out.addAll(local);
    }

    private static void rollPool(JsonObject pool, LootContext ctx, List<ItemStack> out, int depth) {
        if (!conditionsPass(pool, ctx)) return;
        int rolls = (int) Math.floor(number(pool.get("rolls"), ctx, 1) + number(pool.get("bonus_rolls"), ctx, 0) * ctx.getLuck());
        List<ItemStack> local = new ArrayList<>();
        for (int i = 0; i < rolls; i++) {
            List<JsonObject> candidates = new ArrayList<>();
            int total = 0;
            for (JsonElement e : pool.getAsJsonArray("entries")) expand(e.getAsJsonObject(), ctx, candidates);
            for (JsonObject c : candidates) total += weight(c, ctx);
            if (total <= 0) continue;
            int r = ctx.getRandom().nextInt(total);
            for (JsonObject c : candidates) {
                r -= weight(c, ctx);
                if (r < 0) { createEntry(c, ctx, local, depth); break; }
            }
        }
        applyFunctions(pool, local, ctx);
        out.addAll(local);
    }

    private static int weight(JsonObject e, LootContext ctx) {
        int w = e.has("weight") ? e.get("weight").getAsInt() : 1;
        int q = e.has("quality") ? e.get("quality").getAsInt() : 0;
        return Math.max(0, (int) Math.floor(w + q * ctx.getLuck()));
    }

    /** Composite entries become the leaf entries they choose. */
    private static void expand(JsonObject e, LootContext ctx, List<JsonObject> out) {
        if (!conditionsPass(e, ctx)) return;
        String type = type(e);
        switch (type) {
            case "alternatives" -> {
                for (JsonElement c : e.getAsJsonArray("children")) {
                    JsonObject child = c.getAsJsonObject();
                    if (conditionsPass(child, ctx)) { expand(child, ctx, out); return; }
                }
            }
            case "group" -> { for (JsonElement c : e.getAsJsonArray("children")) expand(c.getAsJsonObject(), ctx, out); }
            case "sequence" -> {
                for (JsonElement c : e.getAsJsonArray("children")) {
                    JsonObject child = c.getAsJsonObject();
                    if (!conditionsPass(child, ctx)) return;
                    expand(child, ctx, out);
                }
            }
            case "tag" -> {
                if (e.has("expand") && e.get("expand").getAsBoolean()) {
                    for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.create(ResourceLocation.parse(e.get("name").getAsString())))) {
                        JsonObject leaf = e.deepCopy();
                        leaf.addProperty("type", "minecraft:item");
                        leaf.addProperty("name", h.getRegisteredName());
                        leaf.remove("expand");
                        out.add(leaf);
                    }
                } else out.add(e);
            }
            default -> out.add(e);
        }
    }

    private static void createEntry(JsonObject e, LootContext ctx, List<ItemStack> out, int depth) {
        List<ItemStack> local = new ArrayList<>();
        switch (type(e)) {
            case "item" -> {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.get("name").getAsString()));
                ItemStack s = new ItemStack(item);
                if (!s.isEmpty()) local.add(s);
            }
            case "tag" -> {
                for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.create(ResourceLocation.parse(e.get("name").getAsString())))) {
                    ItemStack s = new ItemStack(h);
                    if (!s.isEmpty()) local.add(s);
                }
            }
            case "loot_table" -> {
                JsonElement v = e.get("value");
                if (v.isJsonPrimitive()) rollTable(TABLES.getOrDefault(ResourceLocation.parse(v.getAsString()), new JsonObject()), ctx, local, depth + 1);
                else rollTable(v.getAsJsonObject(), ctx, local, depth + 1);
            }
            default -> { }
        }
        applyFunctions(e, local, ctx);
        for (ItemStack s : local) {
            if (s.isEmpty()) continue;
            int max = s.getMaxStackSize();
            while (s.getCount() > max) out.add(s.split(max));
            out.add(s);
        }
    }

    private static String type(JsonObject o) {
        String t = o.has("type") ? o.get("type").getAsString() : (o.has("function") ? o.get("function").getAsString() : o.has("condition") ? o.get("condition").getAsString() : "");
        return t.startsWith("minecraft:") ? t.substring(10) : t;
    }

    private static boolean conditionsPass(JsonObject o, LootContext ctx) {
        if (!o.has("conditions")) return true;
        for (JsonElement c : o.getAsJsonArray("conditions")) if (!condition(c).test(ctx)) return false;
        return true;
    }

    private static void applyFunctions(JsonObject o, List<ItemStack> stacks, LootContext ctx) {
        if (!o.has("functions")) return;
        for (JsonElement f : o.getAsJsonArray("functions")) {
            LootItemFunction fn = function(f);
            stacks.replaceAll(s -> fn.apply(s, ctx));
        }
        stacks.removeIf(ItemStack::isEmpty);
    }

    // ------------------------------------------------------------------ number providers

    static double number(JsonElement e, LootContext ctx, double fallback) {
        if (e == null) return fallback;
        if (e.isJsonPrimitive()) return e.getAsDouble();
        JsonObject o = e.getAsJsonObject();
        String t = o.has("type") ? o.get("type").getAsString().replace("minecraft:", "") : (o.has("n") ? "binomial" : "uniform");
        RandomSource r = ctx.getRandom();
        return switch (t) {
            case "constant" -> o.get("value").getAsDouble();
            case "uniform" -> {
                double min = number(o.get("min"), ctx, 0), max = number(o.get("max"), ctx, 1);
                yield min + r.nextDouble() * (max - min);
            }
            case "binomial" -> {
                int n = (int) number(o.get("n"), ctx, 1);
                double p = number(o.get("p"), ctx, 0.5);
                int k = 0;
                for (int i = 0; i < n; i++) if (r.nextDouble() < p) k++;
                yield k;
            }
            default -> fallback;
        };
    }

    private static int intNumber(JsonElement e, LootContext ctx, int fallback) {
        double d = number(e, ctx, fallback);
        return e != null && e.isJsonObject() && !e.getAsJsonObject().has("value") ? (int) Math.round(Math.floor(d + 0.5)) : (int) Math.round(d);
    }

    // ------------------------------------------------------------------ conditions

    private record Cond(LootItemConditionType type, java.util.function.Predicate<LootContext> test) implements LootItemCondition {
        @Override public LootItemConditionType getType() { return type; }
        @Override public boolean test(LootContext ctx) { return test.test(ctx); }
    }

    private static final LootItemConditionType BUILT_IN_CONDITION = new LootItemConditionType(MapCodec.unit(() -> null));

    @SuppressWarnings({"unchecked", "rawtypes"})
    static LootItemCondition condition(JsonElement json) {
        if (json.isJsonArray()) {
            List<LootItemCondition> all = new ArrayList<>();
            for (JsonElement e : json.getAsJsonArray()) all.add(condition(e));
            return new Cond(BUILT_IN_CONDITION, ctx -> all.stream().allMatch(c -> c.test(ctx)));
        }
        JsonObject o = json.getAsJsonObject();
        String type = type(o);
        java.util.function.Predicate<LootContext> p = switch (type) {
            case "survives_explosion" -> ctx -> {
                Float r = ctx.getParamOrNull(LootContextParams.EXPLOSION_RADIUS);
                return r == null || ctx.getRandom().nextFloat() <= 1 / r;
            };
            case "random_chance" -> ctx -> ctx.getRandom().nextFloat() < number(o.get("chance"), ctx, 1);
            case "random_chance_with_enchanted_bonus" -> ctx -> ctx.getRandom().nextFloat() < (o.has("unenchanted_chance") ? o.get("unenchanted_chance").getAsFloat() : 0.1f);
            case "inverted" -> { LootItemCondition c = condition(o.get("term")); yield ctx -> !c.test(ctx); }
            case "any_of", "alternative" -> {
                List<LootItemCondition> l = new ArrayList<>();
                for (JsonElement e : o.getAsJsonArray("terms")) l.add(condition(e));
                yield ctx -> l.stream().anyMatch(c -> c.test(ctx));
            }
            case "all_of" -> {
                List<LootItemCondition> l = new ArrayList<>();
                for (JsonElement e : o.getAsJsonArray("terms")) l.add(condition(e));
                yield ctx -> l.stream().allMatch(c -> c.test(ctx));
            }
            case "match_tool" -> {
                JsonObject pred = o.has("predicate") ? o.getAsJsonObject("predicate") : new JsonObject();
                yield ctx -> itemMatches(pred, ctx.getParamOrNull(LootContextParams.TOOL));
            }
            case "block_state_property" -> {
                JsonObject props = o.has("properties") ? o.getAsJsonObject("properties") : new JsonObject();
                ResourceLocation block = ResourceLocation.parse(o.get("block").getAsString());
                yield ctx -> {
                    BlockState s = ctx.getParamOrNull(LootContextParams.BLOCK_STATE);
                    if (s == null || !s.getBlock().builtInRegistryHolder().is(block)) return false;
                    return stateMatches(s, props);
                };
            }
            case "table_bonus" -> {
                JsonArray chances = o.getAsJsonArray("chances");
                String ench = o.get("enchantment").getAsString();
                yield ctx -> {
                    int level = enchantLevel(ctx.getParamOrNull(LootContextParams.TOOL), ench);
                    return ctx.getRandom().nextFloat() < chances.get(Math.min(level, chances.size() - 1)).getAsFloat();
                };
            }
            case "killed_by_player" -> ctx -> ctx.hasParam(LootContextParams.LAST_DAMAGE_PLAYER);
            case "entity_properties" -> {
                JsonObject pred = o.has("predicate") ? o.getAsJsonObject("predicate") : new JsonObject();
                yield ctx -> {
                    var e = ctx.getParamOrNull(LootContextParams.THIS_ENTITY);
                    if (pred.has("flags") && pred.getAsJsonObject("flags").has("is_on_fire"))
                        return e != null && e.isOnFire() == pred.getAsJsonObject("flags").get("is_on_fire").getAsBoolean();
                    return e != null || pred.size() == 0;
                };
            }
            case "damage_source_properties", "location_check", "weather_check", "time_check", "value_check", "reference", "enchantment_active_check" -> ctx -> true;
            case "neoforge:loot_table_id" -> {
                ResourceLocation id = ResourceLocation.parse(o.get("loot_table_id").getAsString());
                yield ctx -> id.equals(ctx.getQueriedLootTableId());
            }
            case "neoforge:can_item_perform_ability" -> {
                var ability = net.neoforged.neoforge.common.ItemAbility.get(o.get("ability").getAsString());
                yield ctx -> {
                    ItemStack tool = ctx.getParamOrNull(LootContextParams.TOOL);
                    return tool != null && (tool.canPerformAction(ability) || net.neoforged.neoforge.common.ItemAbilities.reamc$engineCan(tool.reamc$handle() == null ? null : tool.reamc$handle().item, ability));
                };
            }
            default -> null;
        };
        if (p != null) return new Cond(BUILT_IN_CONDITION, p);
        // A condition type a mod registered: decode it with its codec
        Object t = BuiltInRegistries.LOOT_CONDITION_TYPE.get(ResourceLocation.parse(o.has("condition") ? o.get("condition").getAsString() : o.get("type").getAsString()));
        if (t instanceof LootItemConditionType ct) return (LootItemCondition) ((MapCodec) ct.codec()).codec().parse(JsonOps.INSTANCE, o).getOrThrow();
        System.err.println("[mods] Unknown loot condition " + type + "; treating it as true");
        return new Cond(BUILT_IN_CONDITION, ctx -> true);
    }

    private static boolean stateMatches(BlockState s, JsonObject props) {
        for (var e : props.entrySet()) {
            Property<?> prop = s.getBlock().getStateDefinition().getProperty(e.getKey());
            if (prop == null) return false;
            String value = propName(s, prop);
            if (e.getValue().isJsonPrimitive()) {
                if (!value.equals(e.getValue().getAsString())) return false;
            } else {
                JsonObject range = e.getValue().getAsJsonObject();
                try {
                    int v = Integer.parseInt(value);
                    if (range.has("min") && v < range.get("min").getAsInt()) return false;
                    if (range.has("max") && v > range.get("max").getAsInt()) return false;
                } catch (NumberFormatException ex) {
                    return false;
                }
            }
        }
        return true;
    }

    private static <T extends Comparable<T>> String propName(BlockState s, Property<T> p) { return p.getName(s.getValue(p)); }

    /** Minecraft's item predicate: items (id, list or #tag), count and enchantments. */
    static boolean itemMatches(JsonObject pred, ItemStack stack) {
        if (stack == null) stack = ItemStack.EMPTY;
        if (pred.has("items")) {
            JsonElement items = pred.get("items");
            boolean ok = false;
            List<String> ids = new ArrayList<>();
            if (items.isJsonArray()) for (JsonElement e : items.getAsJsonArray()) ids.add(e.getAsString());
            else ids.add(items.getAsString());
            for (String id : ids) {
                if (id.startsWith("#")) ok |= stack.is(ItemTags.create(ResourceLocation.parse(id.substring(1))));
                else ok |= !stack.isEmpty() && stack.getItem().builtInRegistryHolder().is(ResourceLocation.parse(id));
            }
            if (!ok) return false;
        }
        if (pred.has("predicates") && pred.getAsJsonObject("predicates").has("minecraft:enchantments")) {
            for (JsonElement e : pred.getAsJsonObject("predicates").getAsJsonArray("minecraft:enchantments")) {
                JsonObject en = e.getAsJsonObject();
                if (!en.has("enchantments")) continue;
                int level = enchantLevel(stack, en.get("enchantments").getAsString());
                int min = en.has("levels") && en.get("levels").isJsonObject() && en.getAsJsonObject("levels").has("min") ? en.getAsJsonObject("levels").get("min").getAsInt() : 1;
                if (level < min) return false;
            }
        }
        return true;
    }

    static int enchantLevel(ItemStack stack, String enchantment) {
        if (stack == null || stack.reamc$handle() == null) return 0;
        String path = ResourceLocation.parse(enchantment).getPath().toUpperCase(java.util.Locale.ROOT);
        try {
            return stack.reamc$handle().level(mc.item.Enchantment.valueOf(path));
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------ functions

    private record Fn(LootItemFunctionType<?> type, java.util.function.BiFunction<ItemStack, LootContext, ItemStack> f) implements LootItemFunction {
        @Override public LootItemFunctionType<? extends LootItemFunction> getType() { return type; }
        @Override public ItemStack apply(ItemStack s, LootContext ctx) { return f.apply(s, ctx); }
    }

    private static final LootItemFunctionType<?> BUILT_IN_FUNCTION = new LootItemFunctionType<>(MapCodec.unit(() -> null));

    @SuppressWarnings({"unchecked", "rawtypes"})
    static LootItemFunction function(JsonElement json) {
        JsonObject o = json.getAsJsonObject();
        String type = type(o);
        List<LootItemCondition> conds = new ArrayList<>();
        if (o.has("conditions")) for (JsonElement c : o.getAsJsonArray("conditions")) conds.add(condition(c));
        java.util.function.BiFunction<ItemStack, LootContext, ItemStack> f = switch (type) {
            case "set_count" -> (s, ctx) -> {
                int n = intNumber(o.get("count"), ctx, 1);
                s.setCount(Math.max(0, (o.has("add") && o.get("add").getAsBoolean() ? s.getCount() : 0) + n));
                return s;
            };
            case "apply_bonus" -> (s, ctx) -> {
                int level = enchantLevel(ctx.getParamOrNull(LootContextParams.TOOL), o.get("enchantment").getAsString());
                if (level <= 0) return s;
                String formula = o.get("formula").getAsString().replace("minecraft:", "");
                JsonObject p = o.has("parameters") ? o.getAsJsonObject("parameters") : new JsonObject();
                RandomSource r = ctx.getRandom();
                int c = s.getCount();
                switch (formula) {
                    case "ore_drops" -> c *= Math.max(1, r.nextInt(level + 2));
                    case "uniform_bonus_count" -> c += r.nextInt((p.has("bonusMultiplier") ? p.get("bonusMultiplier").getAsInt() : 1) * level + 1);
                    case "binomial_with_bonus_count" -> {
                        int extra = p.has("extra") ? p.get("extra").getAsInt() : 0;
                        float prob = p.has("probability") ? p.get("probability").getAsFloat() : 0.5f;
                        for (int i = 0; i < level + extra; i++) if (r.nextFloat() < prob) c++;
                    }
                    default -> { }
                }
                s.setCount(c);
                return s;
            };
            case "limit_count" -> (s, ctx) -> {
                JsonObject lim = o.getAsJsonObject("limit");
                int c = s.getCount();
                if (lim.has("min")) c = Math.max(c, intNumber(lim.get("min"), ctx, c));
                if (lim.has("max")) c = Math.min(c, intNumber(lim.get("max"), ctx, c));
                s.setCount(c);
                return s;
            };
            case "explosion_decay", "set_lore", "set_attributes", "set_banner_pattern", "set_contents", "set_custom_data", "enchant_randomly",
                    "enchant_with_levels", "set_enchantments", "set_potion", "set_stew_effect", "set_instrument", "exploration_map",
                    "set_book_cover", "set_written_book_pages", "set_writable_book_pages", "toggle_tooltips", "set_fireworks",
                    "set_firework_explosion", "copy_custom_data", "fill_player_head", "set_damage", "set_ominous_bottle_amplifier", "reference",
                    "copy_state", "sequence", "filtered", "modify_contents", "set_loot_table", "enchanted_count_increase" -> (s, ctx) -> s;
            case "copy_name" -> (s, ctx) -> {
                var be = ctx.getParamOrNull(LootContextParams.BLOCK_ENTITY);
                if (be instanceof net.minecraft.world.Nameable n && n.hasCustomName()) s.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, n.getCustomName());
                return s;
            };
            case "copy_components" -> (s, ctx) -> {
                var be = ctx.getParamOrNull(LootContextParams.BLOCK_ENTITY);
                if (be == null) return s;
                net.minecraft.core.component.DataComponentMap comps = be.collectComponents();
                List<String> include = new ArrayList<>();
                if (o.has("include")) for (JsonElement e : o.getAsJsonArray("include")) include.add(e.getAsString());
                for (var t : comps) {
                    ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(t.type());
                    if (!o.has("include") || id != null && include.contains(id.toString())) s.set((net.minecraft.core.component.DataComponentType) t.type(), t.value());
                }
                return s;
            };
            case "furnace_smelt" -> (s, ctx) -> {
                mc.item.ItemStack r = s.reamc$handle() == null ? null : mc.item.Recipes.smelting(s.reamc$handle().item);
                return r == null ? s : ItemStack.reamc$wrap(new mc.item.ItemStack(r.item, s.getCount()));
            };
            case "set_components" -> (s, ctx) -> {
                if (!o.has("components")) return s;
                for (var e : o.getAsJsonObject("components").entrySet()) {
                    var t = (net.minecraft.core.component.DataComponentType) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(e.getKey().replace("!", "")));
                    if (t == null || t.codec() == null) continue;
                    if (e.getKey().startsWith("!")) s.remove(t);
                    else t.codec().parse(JsonOps.INSTANCE, e.getValue()).result().ifPresent(v -> s.set(t, v));
                }
                return s;
            };
            case "set_name" -> (s, ctx) -> {
                if (o.has("name")) s.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.Serializer.fromJson(o.get("name"), null));
                return s;
            };
            default -> null;
        };
        if (f == null) {
            Object t = BuiltInRegistries.LOOT_FUNCTION_TYPE.get(ResourceLocation.parse(o.has("function") ? o.get("function").getAsString() : o.get("type").getAsString()));
            if (t instanceof LootItemFunctionType<?> ft) return (LootItemFunction) ((MapCodec) ft.codec()).codec().parse(JsonOps.INSTANCE, o).getOrThrow();
            System.err.println("[mods] Unknown loot function " + type + "; ignoring it");
            return new Fn(BUILT_IN_FUNCTION, (s, ctx) -> s);
        }
        java.util.function.BiFunction<ItemStack, LootContext, ItemStack> base = f;
        return new Fn(BUILT_IN_FUNCTION, (s, ctx) -> conds.stream().allMatch(c -> c.test(ctx)) ? base.apply(s, ctx) : s);
    }
}
