package net.minecraft.world.item;

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
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A stack of items. reamc-compat wraps the engine's stack, so a mod changing a stack changes the stack in the
 * player's inventory or container it came from. Mods' data components live on the engine stack too.
 */
public final class ItemStack implements DataComponentHolder, net.neoforged.neoforge.common.extensions.IItemStackExtension {
    public static final ItemStack EMPTY = new ItemStack((mc.item.ItemStack) null);

    /** Saved form {"id", "count", "components"}, plus reamc's damage and enchantments. */
    public static final Codec<ItemStack> OPTIONAL_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
            try {
                JsonElement j = new Dynamic<>(ops, input).convert(JsonOps.INSTANCE).getValue();
                return DataResult.success(Pair.of(fromJson(j), ops.empty()));
            } catch (RuntimeException e) {
                return DataResult.error(() -> "Bad item stack: " + e.getMessage());
            }
        }

        @Override
        public <T> DataResult<T> encode(ItemStack s, DynamicOps<T> ops, T prefix) {
            return DataResult.success(new Dynamic<>(JsonOps.INSTANCE, s.toJson()).convert(ops).getValue());
        }
    };
    public static final Codec<ItemStack> CODEC = OPTIONAL_CODEC.validate(s -> s.isEmpty() ? DataResult.error(() -> "Item stack must not be empty") : DataResult.success(s));
    public static final Codec<ItemStack> STRICT_CODEC = CODEC;
    public static final Codec<ItemStack> SINGLE_ITEM_CODEC = CODEC.xmap(s -> s.copyWithCount(1), s -> s);
    public static final MapCodec<ItemStack> MAP_CODEC = MapCodec.assumeMapUnsafe(CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(s -> fromJson(com.google.gson.JsonParser.parseString(s)), s -> s.toJson().toString()).cast();
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> STREAM_CODEC = OPTIONAL_STREAM_CODEC;
    public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> OPTIONAL_LIST_STREAM_CODEC = OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.collection(java.util.ArrayList::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> LIST_STREAM_CODEC = OPTIONAL_LIST_STREAM_CODEC;

    private final mc.item.ItemStack handle;

    public ItemStack(ItemLike item) { this(item, 1); }

    public ItemStack(ItemLike item, int count) {
        Item i = item == null ? null : item.asItem();
        mc.item.Item engine = i == null ? null : mc.mod.Bridge.engineItem(i);
        handle = engine == null || count <= 0 ? null : new mc.item.ItemStack(engine, count);
    }

    public ItemStack(Holder<Item> item) { this(item.value(), 1); }

    public ItemStack(Holder<Item> item, int count) { this(item.value(), count); }

    public ItemStack(Holder<Item> item, int count, DataComponentPatch patch) {
        this(item.value(), count);
        if (!isEmpty()) components().applyPatch(patch);
    }

    private ItemStack(mc.item.ItemStack handle) { this.handle = handle; }

    /** The compat view of an engine stack (shares it). */
    public static ItemStack reamc$wrap(mc.item.ItemStack h) { return h == null || h.count <= 0 || h.item == null ? EMPTY : new ItemStack(h); }

    /** The engine stack behind this one, or null when empty. */
    public mc.item.ItemStack reamc$handle() { return isEmpty() ? null : handle; }

    public boolean isEmpty() { return handle == null || handle.count <= 0 || handle.item == null; }

    public Item getItem() { return isEmpty() ? Items.AIR : mc.mod.Bridge.compatItem(handle.item); }

    public Holder<Item> getItemHolder() { return getItem().builtInRegistryHolder(); }

    public boolean is(Item item) { return getItem() == item; }

    public boolean is(TagKey<Item> tag) { return !isEmpty() && getItem().builtInRegistryHolder().is(tag); }

    public boolean is(Predicate<Holder<Item>> p) { return p.test(getItemHolder()); }

    public boolean is(Holder<Item> h) { return getItem() == h.value(); }

    public boolean is(net.minecraft.core.HolderSet<Item> set) { return set.contains(getItemHolder()); }

    public java.util.stream.Stream<TagKey<Item>> getTags() { return getItemHolder().tags(); }

    public int getCount() { return isEmpty() ? 0 : handle.count; }

    public void setCount(int n) { if (handle != null) handle.count = n; }

    public void grow(int n) { setCount(getCount() + n); }

    public void shrink(int n) { setCount(getCount() - n); }

    public void consume(int n, LivingEntity entity) { if (!(entity instanceof Player p) || !p.hasInfiniteMaterials()) shrink(n); }

    public void limitSize(int max) { if (!isEmpty() && getCount() > max) setCount(max); }

    public ItemStack split(int n) {
        int k = Math.min(n, getCount());
        ItemStack s = copyWithCount(k);
        shrink(k);
        return s;
    }

    public ItemStack copy() { return isEmpty() ? EMPTY : new ItemStack(handle.copy()); }

    public ItemStack copyWithCount(int n) {
        if (isEmpty() || n <= 0) return EMPTY;
        mc.item.ItemStack c = handle.copy();
        c.count = n;
        return new ItemStack(c);
    }

    public ItemStack copyAndClear() {
        if (isEmpty()) return EMPTY;
        ItemStack c = copy();
        setCount(0);
        return c;
    }

    public ItemStack transmuteCopy(ItemLike item, int count) {
        if (isEmpty()) return EMPTY;
        ItemStack s = new ItemStack(item, count);
        if (!s.isEmpty()) s.components().applyPatch(components().asPatch());
        return s;
    }

    // ------------------------------------------------------------------ components

    /** The stack's component map (item defaults plus this stack's changes), created on the engine stack when needed. */
    private PatchedDataComponentMap components() {
        DataComponentMap proto = mc.mod.Bridge.compatItem(handle.item).components();
        if (handle.components instanceof PatchedDataComponentMap m) {
            PatchedDataComponentMap bound = m.reamc$withPrototype(proto);
            if (bound != m) handle.components = bound;
            return bound;
        }
        PatchedDataComponentMap m = new PatchedDataComponentMap(proto);
        handle.components = m;
        return m;
    }

    @Override
    public DataComponentMap getComponents() { return isEmpty() ? DataComponentMap.EMPTY : components(); }

    public DataComponentMap getPrototype() { return isEmpty() ? DataComponentMap.EMPTY : getItem().components(); }

    public DataComponentPatch getComponentsPatch() { return isEmpty() ? DataComponentPatch.EMPTY : components().asPatch(); }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(DataComponentType<? extends T> type) {
        if (isEmpty()) return null;
        if (type == DataComponents.DAMAGE) return (T) Integer.valueOf(handle.damage);
        if (type == DataComponents.MAX_DAMAGE) return handle.item.maxDamage > 0 ? (T) Integer.valueOf(handle.item.maxDamage) : components().get(type);
        if (type == DataComponents.MAX_STACK_SIZE) return (T) Integer.valueOf(handle.item.maxStack);
        return components().get(type);
    }

    @SuppressWarnings("unchecked")
    public <T> T set(DataComponentType<? super T> type, T value) {
        if (isEmpty()) return null;
        if (type == DataComponents.DAMAGE) {
            T old = (T) Integer.valueOf(handle.damage);
            handle.damage = value == null ? 0 : (Integer) value;
            return old;
        }
        return components().set(type, value);
    }

    public <T> T set(Supplier<? extends DataComponentType<? super T>> type, T value) { return set(type.get(), value); }

    public <T, U> T update(DataComponentType<T> type, T fallback, U arg, java.util.function.BiFunction<T, U, T> f) { return set(type, f.apply(getOrDefault(type, fallback), arg)); }

    public <T> T update(DataComponentType<T> type, T fallback, UnaryOperator<T> f) { return set(type, f.apply(getOrDefault(type, fallback))); }

    public <T> T remove(DataComponentType<? extends T> type) { return isEmpty() ? null : components().remove(type); }

    public <T> T remove(Supplier<? extends DataComponentType<? extends T>> type) { return remove(type.get()); }

    public void applyComponents(DataComponentMap map) { if (!isEmpty()) components().setAll(map); }

    public void applyComponents(DataComponentPatch patch) { if (!isEmpty()) components().applyPatch(patch); }

    public void applyComponentsAndValidate(DataComponentPatch patch) { applyComponents(patch); }

    public <T> T getOrDefault(Supplier<? extends DataComponentType<? extends T>> type, T fallback) { return getOrDefault(type.get(), fallback); }

    public <T> T get(Supplier<? extends DataComponentType<? extends T>> type) { return get(type.get()); }

    public boolean has(Supplier<? extends DataComponentType<?>> type) { return has(type.get()); }

    // ------------------------------------------------------------------ properties

    public int getMaxStackSize() { return isEmpty() ? 64 : getItem().getMaxStackSize(this); }

    public boolean isStackable() { return getMaxStackSize() > 1 && (!isDamageableItem() || !isDamaged()); }

    public boolean isDamageableItem() { return !isEmpty() && getMaxDamage() > 0; }

    public boolean isDamaged() { return isDamageableItem() && handle.damage > 0; }

    public int getDamageValue() { return isEmpty() ? 0 : handle.damage; }

    public void setDamageValue(int d) { if (handle != null) handle.damage = Math.max(0, Math.min(getMaxDamage(), d)); }

    public int getMaxDamage() {
        if (isEmpty()) return 0;
        Integer d = get(DataComponents.MAX_DAMAGE);
        return d == null ? 0 : d;
    }

    public boolean isEnchanted() { return !isEmpty() && handle.isEnchanted(); }

    public boolean isEnchantable() { return !isEmpty() && getItem().isEnchantable(this) && !isEnchanted(); }

    public boolean hasFoil() { return !isEmpty() && getItem().isFoil(this); }

    public Rarity getRarity() {
        Rarity r = getOrDefault(DataComponents.RARITY, Rarity.COMMON);
        if (!isEnchanted()) return r;
        return switch (r) { case COMMON, UNCOMMON -> Rarity.RARE; case RARE -> Rarity.EPIC; default -> r; };
    }

    public boolean isItemEnabled(net.minecraft.world.flag.FeatureFlagSet flags) { return true; }

    public FoodProperties getFoodProperties(LivingEntity entity) { return isEmpty() ? null : getItem().getFoodProperties(this, entity); }

    public boolean canPerformAction(net.neoforged.neoforge.common.ItemAbility ability) { return !isEmpty() && getItem().canPerformAction(this, ability); }

    public ItemStack getCraftingRemainingItem() { return isEmpty() ? EMPTY : getItem().getCraftingRemainingItem(this); }

    public boolean hasCraftingRemainingItem() { return !isEmpty() && getItem().hasCraftingRemainingItem(this); }

    public int getBurnTime(net.minecraft.world.item.crafting.RecipeType<?> type) { return isEmpty() ? 0 : getItem().getBurnTime(this, type); }

    @Override
    public int getEnchantmentLevel(Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment) {
        if (isEmpty()) return 0;
        mc.item.Enchantment e = mc.mod.Bridge.engineEnchantment(enchantment);
        return e == null ? 0 : handle.level(e);
    }

    public Component getHoverName() {
        Component custom = get(DataComponents.CUSTOM_NAME);
        if (custom != null) return custom;
        if (isEmpty()) return Component.translatable("block.minecraft.air");
        if (!handle.item.key().startsWith("minecraft:")) return getItem().getName(this);
        return Component.literal(mc.item.Item.displayName(handle));
    }

    public Component getDisplayName() { return Component.literal("[" + getHoverName().getString() + "]"); }

    public List<Component> getTooltipLines(Item.TooltipContext context, Player player, TooltipFlag flag) {
        List<Component> l = new java.util.ArrayList<>();
        l.add(getHoverName().copy().withStyle(getRarity().color()));
        if (!isEmpty()) getItem().appendHoverText(this, context, l, flag);
        return l;
    }

    public boolean hasCustomHoverName() { return has(DataComponents.CUSTOM_NAME); }

    // ------------------------------------------------------------------ actions

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) { return getItem().use(level, player, hand); }

    public InteractionResult useOn(UseOnContext context) { return getItem().useOn(context); }

    public InteractionResult onItemUseFirst(UseOnContext context) { return getItem().onItemUseFirst(this, context); }

    public ItemStack finishUsingItem(Level level, LivingEntity entity) { return getItem().finishUsingItem(this, level, entity); }

    public int getUseDuration(LivingEntity entity) { return isEmpty() ? 0 : getItem().getUseDuration(this, entity); }

    public UseAnim getUseAnimation() { return isEmpty() ? UseAnim.NONE : getItem().getUseAnimation(this); }

    public void releaseUsing(Level level, LivingEntity entity, int timeLeft) { if (!isEmpty()) getItem().releaseUsing(this, level, entity, timeLeft); }

    public void onUseTick(Level level, LivingEntity entity, int remaining) { if (!isEmpty()) getItem().onUseTick(level, entity, this, remaining); }

    public void inventoryTick(Level level, Entity entity, int slot, boolean selected) { if (!isEmpty()) getItem().inventoryTick(this, level, entity, slot, selected); }

    public InteractionResult interactLivingEntity(Player player, LivingEntity target, InteractionHand hand) { return getItem().interactLivingEntity(this, player, target, hand); }

    public boolean hurtEnemy(LivingEntity target, Player attacker) { return getItem().hurtEnemy(this, target, attacker); }

    public void postHurtEnemy(LivingEntity target, Player attacker) { getItem().postHurtEnemy(this, target, attacker); }

    public void mineBlock(Level level, BlockState state, net.minecraft.core.BlockPos pos, Player player) { getItem().mineBlock(this, level, state, pos, player); }

    public float getDestroySpeed(BlockState state) { return getItem().getDestroySpeed(this, state); }

    public boolean isCorrectToolForDrops(BlockState state) { return getItem().isCorrectToolForDrops(this, state); }

    public void onCraftedBy(Level level, Player player, int amount) { getItem().onCraftedBy(this, level, player); }

    /** Wears the item down; at zero it breaks (calling onBroken with its item). */
    public void hurtAndBreak(int amount, ServerLevel level, LivingEntity entity, Consumer<Item> onBroken) {
        if (!isDamageableItem() || entity instanceof Player p && p.hasInfiniteMaterials()) return;
        amount = getItem().damageItem(this, amount, entity, onBroken);
        if (amount <= 0) return;
        if (handle.damageTool(amount)) {
            Item item = getItem();
            shrink(1);
            onBroken.accept(item);
            setDamageValue(0);
        }
    }

    public void hurtAndBreak(int amount, LivingEntity entity, EquipmentSlot slot) {
        if (entity.level() instanceof ServerLevel sl) hurtAndBreak(amount, sl, entity, i -> entity.onEquippedItemBroken(i, slot));
    }

    // ------------------------------------------------------------------ comparison

    public static boolean isSameItem(ItemStack a, ItemStack b) { return a.getItem() == b.getItem(); }

    public static boolean isSameItemSameComponents(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return a.isEmpty() && b.isEmpty();
        return a.getItem() == b.getItem() && a.handle.damage == b.handle.damage && a.handle.sameComponents(b.handle)
                && java.util.Objects.equals(a.handle.enchants, b.handle.enchants);
    }

    public static boolean matches(ItemStack a, ItemStack b) {
        return a == b || (a.getCount() == b.getCount() && isSameItemSameComponents(a, b));
    }

    public static boolean isSameItemSameTags(ItemStack a, ItemStack b) { return isSameItemSameComponents(a, b); }

    public static int hashItemAndComponents(ItemStack s) {
        if (s == null || s.isEmpty()) return 0;
        return 31 * s.getItem().hashCode() + (s.handle.components == null ? 0 : s.handle.components.hashCode());
    }

    public static boolean listMatches(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (!matches(a.get(i), b.get(i))) return false;
        return true;
    }

    // ------------------------------------------------------------------ saving

    private JsonObject toJson() {
        JsonObject o = new JsonObject();
        if (isEmpty()) return o;
        o.addProperty("id", getItem().builtInRegistryHolder().unwrapKey().map(k -> k.location().toString()).orElse(handle.item.key()));
        o.addProperty("count", handle.count);
        if (handle.damage != 0 || handle.isEnchanted()) {
            JsonArray a = new JsonArray();
            for (int v : handle.toArray()) a.add(v);
            o.add("reamc", a);
        }
        if (handle.components instanceof PatchedDataComponentMap m && !m.isEmpty()) o.add("components", com.google.gson.JsonParser.parseString(m.save()));
        return o;
    }

    private static ItemStack fromJson(JsonElement j) {
        if (j == null || !j.isJsonObject() || !j.getAsJsonObject().has("id")) return EMPTY;
        JsonObject o = j.getAsJsonObject();
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(o.get("id").getAsString()));
        mc.item.Item engine = item == null ? null : mc.mod.Bridge.engineItem(item);
        if (engine == null) return EMPTY;
        int count = o.has("count") ? o.get("count").getAsInt() : 1;
        mc.item.ItemStack h;
        if (o.has("reamc")) {
            JsonArray a = o.getAsJsonArray("reamc");
            int[] arr = new int[a.size()];
            for (int i = 0; i < arr.length; i++) arr[i] = a.get(i).getAsInt();
            h = mc.item.ItemStack.fromArray(arr);
            if (h == null) return EMPTY;
            h.count = count;
        } else h = new mc.item.ItemStack(engine, count);
        if (o.has("components")) h.components = mc.item.ItemComponents.parse(o.get("components").toString());
        return reamc$wrap(h);
    }

    public Tag save(HolderLookup.Provider registries) {
        if (isEmpty()) throw new IllegalStateException("Cannot encode empty ItemStack");
        return reamc$save();
    }

    public Tag save(HolderLookup.Provider registries, Tag prefix) {
        CompoundTag t = reamc$save();
        if (prefix instanceof CompoundTag c) { c.merge(t); return c; }
        return t;
    }

    public Tag saveOptional(HolderLookup.Provider registries) { return isEmpty() ? new CompoundTag() : save(registries); }

    public static Optional<ItemStack> parse(HolderLookup.Provider registries, Tag tag) {
        ItemStack s = tag instanceof CompoundTag c ? reamc$load(c) : EMPTY;
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }

    public static ItemStack parseOptional(HolderLookup.Provider registries, CompoundTag tag) { return tag.isEmpty() ? EMPTY : reamc$load(tag); }

    /** Saved form as NBT (the JSON form, plus reamc's full stack data). */
    public CompoundTag reamc$save() {
        CompoundTag c = new CompoundTag();
        if (isEmpty()) return c;
        JsonObject j = toJson();
        c.putString("id", j.get("id").getAsString());
        c.putInt("count", handle.count);
        c.putIntArray("reamc", handle.toArray());
        return c;
    }

    public static ItemStack reamc$load(CompoundTag c) {
        if (c.contains("reamc")) return reamc$wrap(mc.item.ItemStack.fromArray(c.getIntArray("reamc")));
        if (!c.contains("id")) return EMPTY;
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(c.getString("id")));
        mc.item.Item it = item == null ? null : mc.mod.Bridge.engineItem(item);
        return it == null ? EMPTY : reamc$wrap(new mc.item.ItemStack(it, Math.max(1, c.getInt("count"))));
    }

    public <T> T getCapability(net.neoforged.neoforge.capabilities.ItemCapability<T, Void> capability) { return capability.reamc$get(this, null); }

    public <T, C> T getCapability(net.neoforged.neoforge.capabilities.ItemCapability<T, C> capability, C context) { return capability.reamc$get(this, context); }

    @Override public String toString() { return getCount() + " " + getItem(); }
}
