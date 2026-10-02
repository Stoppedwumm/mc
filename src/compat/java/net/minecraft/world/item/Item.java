package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * An item (reamc-compat). Mods' items get an engine item when registered; reamc calls the overridable methods
 * below (use, useOn, finishUsingItem, inventoryTick...) from its interaction code.
 */
public class Item implements ItemLike, net.minecraft.world.flag.FeatureElement {
    public static final ResourceLocation BASE_ATTACK_DAMAGE_ID = ResourceLocation.withDefaultNamespace("base_attack_damage");
    public static final ResourceLocation BASE_ATTACK_SPEED_ID = ResourceLocation.withDefaultNamespace("base_attack_speed");
    public static final int DEFAULT_MAX_STACK_SIZE = 64, ABSOLUTE_MAX_STACK_SIZE = 99, MAX_BAR_WIDTH = 13;

    final Properties properties;
    private final DataComponentMap components;
    private final Item craftingRemainingItem;
    /** The engine item this stands for (set when registered). */
    public mc.item.Item reamc$item;
    private Holder.Reference<Item> holder = Holder.Reference.createIntrusive(null, this);
    String descriptionId;

    public Item(Properties properties) {
        this.properties = properties;
        this.components = properties.buildComponents();
        this.craftingRemainingItem = properties.craftRemainder;
    }

    public void reamc$bindHolder(Holder.Reference<Item> h) { holder = h; }

    @Override public Item asItem() { return this; }

    public Properties reamc$properties() { return properties; }

    public Holder.Reference<Item> builtInRegistryHolder() { return holder; }

    public DataComponentMap components() { return components; }

    public int getDefaultMaxStackSize() { return components.getOrDefault(DataComponents.MAX_STACK_SIZE, 1); }

    // ------------------------------------------------------------------ names

    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
            descriptionId = id == null ? "item.unregistered" : id.toLanguageKey("item");
        }
        return descriptionId;
    }

    public String getDescriptionId() { return getOrCreateDescriptionId(); }
    public String getDescriptionId(ItemStack stack) { return getDescriptionId(); }
    public Component getDescription() { return Component.translatable(getDescriptionId()); }
    public Component getName(ItemStack stack) { return Component.translatable(getDescriptionId(stack)); }
    public ItemStack getDefaultInstance() { return new ItemStack(this); }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) { }

    public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(ItemStack stack) { return java.util.Optional.empty(); }

    public boolean isFoil(ItemStack stack) { return stack.isEnchanted(); }

    // ------------------------------------------------------------------ using

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        FoodProperties food = stack.getFoodProperties(player);
        if (food != null) {
            if (player.canEat(food.canAlwaysEat())) {
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(stack);
            }
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    public InteractionResult useOn(UseOnContext context) { return InteractionResult.PASS; }

    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) { return InteractionResult.PASS; }

    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) { return InteractionResult.PASS; }

    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        FoodProperties food = stack.getFoodProperties(entity);
        return food != null ? entity.eat(level, stack, food) : stack;
    }

    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        FoodProperties food = stack.getFoodProperties(entity);
        return food != null ? food.eatDurationTicks() : 0;
    }

    public UseAnim getUseAnimation(ItemStack stack) { return stack.getFoodProperties(null) != null ? UseAnim.EAT : UseAnim.NONE; }

    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) { }

    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) { }

    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) { }

    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) { }

    public void onCraftedBy(ItemStack stack, Level level, Player player) { }

    public void onCraftedPostProcess(ItemStack stack, Level level) { }

    public boolean useOnRelease(ItemStack stack) { return false; }

    public net.minecraft.sounds.SoundEvent getEatingSound() { return net.minecraft.sounds.SoundEvents.GENERIC_EAT; }

    public net.minecraft.sounds.SoundEvent getDrinkingSound() { return net.minecraft.sounds.SoundEvents.GENERIC_DRINK; }

    // ------------------------------------------------------------------ combat and mining

    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) { return false; }

    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) { }

    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) { return false; }

    public float getDestroySpeed(ItemStack stack, BlockState state) { return 1; }

    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) { return false; }

    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) { return true; }

    public float getAttackDamageBonus(Entity target, float damage, net.minecraft.world.damagesource.DamageSource source) { return 0; }

    // ------------------------------------------------------------------ bars, repair, enchanting

    public boolean isBarVisible(ItemStack stack) { return stack.isDamaged(); }

    public int getBarWidth(ItemStack stack) { return Math.round(MAX_BAR_WIDTH - stack.getDamageValue() * 13f / stack.getMaxDamage()); }

    public int getBarColor(ItemStack stack) {
        float f = Math.max(0, (stack.getMaxDamage() - stack.getDamageValue()) / (float) stack.getMaxDamage());
        return java.awt.Color.HSBtoRGB(f / 3f, 1, 1) & 0xFFFFFF;
    }

    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) { return false; }

    public boolean isEnchantable(ItemStack stack) { return stack.getMaxStackSize() == 1 && stack.has(DataComponents.MAX_DAMAGE); }

    public int getEnchantmentValue() { return 0; }

    public int getEnchantmentValue(ItemStack stack) { return getEnchantmentValue(); }

    public boolean supportsEnchantment(ItemStack stack, Holder<?> enchantment) { return isEnchantable(stack); }

    public boolean isPrimaryItemFor(ItemStack stack, Holder<?> enchantment) { return supportsEnchantment(stack, enchantment); }

    public boolean isBookEnchantable(ItemStack stack, ItemStack book) { return true; }

    public boolean isRepairable(ItemStack stack) { return isDamageable(stack); }

    public boolean isDamageable(ItemStack stack) { return stack.has(DataComponents.MAX_DAMAGE) && getMaxDamage(stack) > 0; }

    public int getMaxDamage(ItemStack stack) { return stack.getOrDefault(DataComponents.MAX_DAMAGE, 0); }

    public int getDamage(ItemStack stack) { return stack.getDamageValue(); }

    public void setDamage(ItemStack stack, int damage) { stack.setDamageValue(damage); }

    public boolean isDamaged(ItemStack stack) { return stack.getDamageValue() > 0; }

    public int damageItem(ItemStack stack, int amount, LivingEntity entity, java.util.function.Consumer<Item> onBroken) { return amount; }

    // ------------------------------------------------------------------ NeoForge item extensions

    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return craftingRemainingItem == null ? ItemStack.EMPTY : new ItemStack(craftingRemainingItem);
    }

    public boolean hasCraftingRemainingItem(ItemStack stack) { return !getCraftingRemainingItem(stack).isEmpty(); }

    public Item getCraftingRemainingItem() { return craftingRemainingItem; }

    public boolean hasCraftingRemainingItem() { return craftingRemainingItem != null; }

    public FoodProperties getFoodProperties(ItemStack stack, LivingEntity entity) { return stack.get(DataComponents.FOOD); }

    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility ability) { return false; }

    public int getMaxStackSize(ItemStack stack) { return stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 1); }

    public int getBurnTime(ItemStack stack, net.minecraft.world.item.crafting.RecipeType<?> type) { return -1; }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) { return slotChanged || !ItemStack.isSameItem(oldStack, newStack); }

    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) { return !ItemStack.isSameItem(oldStack, newStack); }

    public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) { return ItemStack.isSameItem(oldStack, newStack); }

    public boolean onDroppedByPlayer(ItemStack stack, Player player) { return true; }

    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) { return false; }

    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) { return false; }

    public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) { return false; }

    public EquipmentSlot getEquipmentSlot(ItemStack stack) { return null; }

    public boolean canEquip(ItemStack stack, EquipmentSlot slot, LivingEntity entity) { return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND; }

    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) { return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY); }

    public String getCreatorModId(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        return id == null ? null : id.getNamespace();
    }

    public boolean canFitInsideContainerItems(ItemStack stack) { return true; }

    public boolean canFitInsideContainerItems() { return true; }

    public boolean overrideStackedOnOther(ItemStack stack, net.minecraft.world.inventory.Slot slot, net.minecraft.world.inventory.ClickAction action, Player player) { return false; }

    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, net.minecraft.world.inventory.Slot slot, net.minecraft.world.inventory.ClickAction action, Player player, net.minecraft.world.entity.SlotAccess access) { return false; }

    public void verifyComponentsAfterLoad(ItemStack stack) { }

    public boolean isComplex() { return false; }

    @Override public net.minecraft.world.flag.FeatureFlagSet requiredFeatures() { return net.minecraft.world.flag.FeatureFlagSet.of(); }

    public static Item byId(int id) { return BuiltInRegistries.ITEM.byId(id); }

    public static int getId(Item item) { return item == null ? 0 : BuiltInRegistries.ITEM.getId(item); }

    public static Item byBlock(net.minecraft.world.level.block.Block block) { return block.asItem(); }

    @Override public String toString() { return String.valueOf(BuiltInRegistries.ITEM.getKey(this)); }

    /** What a tooltip may look things up in (reamc-compat). */
    public interface TooltipContext {
        TooltipContext EMPTY = new TooltipContext() { };

        default HolderLookup.Provider registries() { return HolderLookup.Provider.EMPTY; }
        default float tickRate() { return 20; }
        default net.minecraft.world.level.saveddata.maps.MapItemSavedData mapData(Object mapId) { return null; }

        static TooltipContext of(Level level) { return EMPTY; }
        static TooltipContext of(HolderLookup.Provider registries) { return EMPTY; }
    }

    /** Item settings, kept as the item's default components. */
    public static class Properties {
        int maxStack = 64, maxDamage;
        Rarity rarity = Rarity.COMMON;
        ItemAttributeModifiers attributes;
        FoodProperties food;
        boolean fireResistant;
        Item craftRemainder;
        private final DataComponentMap.Builder components = DataComponentMap.builder();

        public Properties() { }

        public Properties stacksTo(int n) { maxStack = n; return this; }
        public Properties durability(int d) { maxDamage = d; maxStack = 1; return this; }
        public Properties rarity(Rarity r) { rarity = r; return this; }
        public Properties attributes(ItemAttributeModifiers a) { attributes = a; return this; }
        public Properties food(FoodProperties f) { food = f; return this; }
        public Properties fireResistant() { fireResistant = true; return this; }
        public Properties craftRemainder(Item i) { craftRemainder = i; return this; }
        public Properties setNoRepair() { return this; }
        public Properties requiredFeatures(net.minecraft.world.flag.FeatureFlag... flags) { return this; }
        public Properties jukeboxPlayable(net.minecraft.resources.ResourceKey<?> song) { return this; }
        public <T> Properties component(DataComponentType<T> type, T value) { components.set(type, value); return this; }

        DataComponentMap buildComponents() {
            DataComponentMap.Builder b = DataComponentMap.builder();
            b.set(DataComponents.MAX_STACK_SIZE, maxStack);
            if (maxDamage > 0) { b.set(DataComponents.MAX_DAMAGE, maxDamage); b.set(DataComponents.DAMAGE, 0); }
            b.set(DataComponents.RARITY, rarity);
            if (attributes != null) b.set(DataComponents.ATTRIBUTE_MODIFIERS, attributes);
            if (food != null) b.set(DataComponents.FOOD, food);
            if (fireResistant) b.set(DataComponents.FIRE_RESISTANT, net.minecraft.util.Unit.INSTANCE);
            b.addAll(components.build());
            return b.build();
        }

        public int reamc$maxStack() { return maxStack; }
        public int reamc$maxDamage() { return maxDamage; }
        public Rarity reamc$rarity() { return rarity; }
        public ItemAttributeModifiers reamc$attributes() { return attributes; }
        public FoodProperties reamc$food() { return food; }
    }
}
