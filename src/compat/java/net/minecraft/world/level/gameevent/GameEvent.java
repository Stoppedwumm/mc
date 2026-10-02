package net.minecraft.world.level.gameevent;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/** Vibrations for sculk sensors (reamc-compat: reamc has no listeners, so events go nowhere). */
@SuppressWarnings({"unchecked", "rawtypes"})
public record GameEvent(int notificationRadius) {
    private static Holder.Reference<GameEvent> register(String id) {
        return Registry.registerForHolder((Registry) BuiltInRegistries.GAME_EVENT, ResourceLocation.withDefaultNamespace(id), new GameEvent(16));
    }

    public static final Holder.Reference<GameEvent> BLOCK_ACTIVATE = register("block_activate"), BLOCK_ATTACH = register("block_attach"),
            BLOCK_CHANGE = register("block_change"), BLOCK_CLOSE = register("block_close"), BLOCK_DEACTIVATE = register("block_deactivate"),
            BLOCK_DESTROY = register("block_destroy"), BLOCK_DETACH = register("block_detach"), BLOCK_OPEN = register("block_open"),
            BLOCK_PLACE = register("block_place"), CONTAINER_CLOSE = register("container_close"), CONTAINER_OPEN = register("container_open"),
            DRINK = register("drink"), EAT = register("eat"), ENTITY_DAMAGE = register("entity_damage"), ENTITY_DIE = register("entity_die"),
            ENTITY_INTERACT = register("entity_interact"), ENTITY_PLACE = register("entity_place"), EQUIP = register("equip"),
            HIT_GROUND = register("hit_ground"), ITEM_INTERACT_FINISH = register("item_interact_finish"),
            ITEM_INTERACT_START = register("item_interact_start"), PROJECTILE_LAND = register("projectile_land"),
            PROJECTILE_SHOOT = register("projectile_shoot"), SHEAR = register("shear"), SPLASH = register("splash"), STEP = register("step"),
            SWIM = register("swim"), TELEPORT = register("teleport");

    public record Context(Entity sourceEntity, BlockState affectedState) {
        public static Context of(Entity e) { return new Context(e, null); }
        public static Context of(BlockState s) { return new Context(null, s); }
        public static Context of(Entity e, BlockState s) { return new Context(e, s); }
    }
}
