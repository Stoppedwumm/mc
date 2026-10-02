package net.minecraft.world.level.storage.loot.parameters;

import static net.minecraft.world.level.storage.loot.parameters.LootContextParams.*;

public class LootContextParamSets {
    public static final LootContextParamSet EMPTY = LootContextParamSet.builder().build();
    public static final LootContextParamSet CHEST = LootContextParamSet.builder().required(ORIGIN).optional(THIS_ENTITY).build();
    public static final LootContextParamSet COMMAND = LootContextParamSet.builder().required(ORIGIN).optional(THIS_ENTITY).build();
    public static final LootContextParamSet SELECTOR = LootContextParamSet.builder().required(ORIGIN).required(THIS_ENTITY).build();
    public static final LootContextParamSet FISHING = LootContextParamSet.builder().required(ORIGIN).required(TOOL).optional(THIS_ENTITY).build();
    public static final LootContextParamSet ENTITY = LootContextParamSet.builder().required(THIS_ENTITY).required(ORIGIN).required(DAMAGE_SOURCE).optional(ATTACKING_ENTITY).optional(DIRECT_ATTACKING_ENTITY).optional(LAST_DAMAGE_PLAYER).build();
    public static final LootContextParamSet EQUIPMENT = LootContextParamSet.builder().required(ORIGIN).required(THIS_ENTITY).build();
    public static final LootContextParamSet ARCHAEOLOGY = LootContextParamSet.builder().required(ORIGIN).optional(THIS_ENTITY).build();
    public static final LootContextParamSet GIFT = LootContextParamSet.builder().required(ORIGIN).required(THIS_ENTITY).build();
    public static final LootContextParamSet PIGLIN_BARTER = LootContextParamSet.builder().required(THIS_ENTITY).build();
    public static final LootContextParamSet VAULT = LootContextParamSet.builder().required(ORIGIN).optional(THIS_ENTITY).build();
    public static final LootContextParamSet ADVANCEMENT_REWARD = LootContextParamSet.builder().required(THIS_ENTITY).required(ORIGIN).build();
    public static final LootContextParamSet ADVANCEMENT_ENTITY = LootContextParamSet.builder().required(THIS_ENTITY).required(ORIGIN).build();
    public static final LootContextParamSet ADVANCEMENT_LOCATION = LootContextParamSet.builder().required(THIS_ENTITY).required(ORIGIN).required(TOOL).required(BLOCK_STATE).build();
    public static final LootContextParamSet BLOCK_USE = LootContextParamSet.builder().required(THIS_ENTITY).required(ORIGIN).required(BLOCK_STATE).build();
    public static final LootContextParamSet ALL_PARAMS = LootContextParamSet.builder().optional(THIS_ENTITY).optional(LAST_DAMAGE_PLAYER).optional(DAMAGE_SOURCE).optional(ATTACKING_ENTITY).optional(DIRECT_ATTACKING_ENTITY).optional(ORIGIN).optional(BLOCK_STATE).optional(BLOCK_ENTITY).optional(TOOL).optional(EXPLOSION_RADIUS).build();
    public static final LootContextParamSet BLOCK = LootContextParamSet.builder().required(BLOCK_STATE).required(ORIGIN).required(TOOL).optional(THIS_ENTITY).optional(BLOCK_ENTITY).optional(EXPLOSION_RADIUS).build();
    public static final LootContextParamSet SHEARING = LootContextParamSet.builder().required(ORIGIN).optional(THIS_ENTITY).build();
    public static final LootContextParamSet ENCHANTED_DAMAGE = LootContextParamSet.builder().required(THIS_ENTITY).required(ENCHANTMENT_LEVEL).required(ORIGIN).required(DAMAGE_SOURCE).optional(DIRECT_ATTACKING_ENTITY).optional(ATTACKING_ENTITY).build();
    public static final LootContextParamSet ENCHANTED_ITEM = LootContextParamSet.builder().required(TOOL).required(ENCHANTMENT_LEVEL).build();
    public static final LootContextParamSet ENCHANTED_LOCATION = LootContextParamSet.builder().required(THIS_ENTITY).required(ENCHANTMENT_LEVEL).required(ORIGIN).required(ENCHANTMENT_ACTIVE).build();
    public static final LootContextParamSet ENCHANTED_ENTITY = LootContextParamSet.builder().required(THIS_ENTITY).required(ENCHANTMENT_LEVEL).required(ORIGIN).build();
    public static final LootContextParamSet HIT_BLOCK = LootContextParamSet.builder().required(THIS_ENTITY).required(ENCHANTMENT_LEVEL).required(ORIGIN).required(BLOCK_STATE).build();
}
