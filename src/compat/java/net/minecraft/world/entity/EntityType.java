package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * A kind of entity (reamc-compat). reamc's own entities have types for comparisons; mods' types create their
 * entities through the factory.
 */
public class EntityType<T extends Entity> implements net.minecraft.world.flag.FeatureElement {
    private final EntityFactory<T> factory;
    private final MobCategory category;
    private final float width, height;
    private final boolean fireImmune;
    private final int clientTrackingRange, updateInterval;
    private String descriptionId;

    public EntityType(EntityFactory<T> factory, MobCategory category, float width, float height, boolean fireImmune, int trackingRange, int updateInterval) {
        this.factory = factory;
        this.category = category;
        this.width = width;
        this.height = height;
        this.fireImmune = fireImmune;
        this.clientTrackingRange = trackingRange;
        this.updateInterval = updateInterval;
    }

    public T create(Level level) { return factory == null ? null : factory.create(this, level); }

    public T spawn(net.minecraft.server.level.ServerLevel level, BlockPos pos, MobSpawnType reason) {
        T e = create(level);
        if (e == null) return null;
        e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(e);
        return e;
    }

    public MobCategory getCategory() { return category; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public boolean fireImmune() { return fireImmune; }
    public int clientTrackingRange() { return clientTrackingRange; }
    public int updateInterval() { return updateInterval; }
    public EntityDimensions getDimensions() { return EntityDimensions.scalable(width, height); }

    public String getDescriptionId() {
        if (descriptionId == null) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(this);
            descriptionId = id == null ? "entity.unregistered" : id.toLanguageKey("entity");
        }
        return descriptionId;
    }

    public Component getDescription() { return Component.translatable(getDescriptionId()); }

    public boolean is(net.minecraft.tags.TagKey<EntityType<?>> tag) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(this);
        return id != null && mc.mod.Tags.has(tag, id);
    }

    @Override public net.minecraft.world.flag.FeatureFlagSet requiredFeatures() { return net.minecraft.world.flag.FeatureFlagSet.of(); }

    @Override public String toString() { return "EntityType[" + BuiltInRegistries.ENTITY_TYPE.getKey(this) + "]"; }

    @FunctionalInterface
    public interface EntityFactory<T extends Entity> {
        T create(EntityType<T> type, Level level);
    }

    public static class Builder<T extends Entity> {
        private final EntityFactory<T> factory;
        private final MobCategory category;
        private float width = 0.6f, height = 1.8f;
        private boolean fireImmune;
        private int trackingRange = 5, updateInterval = 3;

        private Builder(EntityFactory<T> factory, MobCategory category) { this.factory = factory; this.category = category; }

        public static <T extends Entity> Builder<T> of(EntityFactory<T> factory, MobCategory category) { return new Builder<>(factory, category); }

        public static <T extends Entity> Builder<T> createNothing(MobCategory category) { return new Builder<>((t, l) -> null, category); }

        public Builder<T> sized(float w, float h) { width = w; height = h; return this; }
        public Builder<T> spawnDimensionsScale(float f) { return this; }
        public Builder<T> eyeHeight(float f) { return this; }
        public Builder<T> passengerAttachments(float... f) { return this; }
        public Builder<T> passengerAttachments(net.minecraft.world.phys.Vec3... v) { return this; }
        public Builder<T> vehicleAttachment(net.minecraft.world.phys.Vec3 v) { return this; }
        public Builder<T> ridingOffset(float f) { return this; }
        public Builder<T> nameTagOffset(float f) { return this; }
        public Builder<T> noSummon() { return this; }
        public Builder<T> noSave() { return this; }
        public Builder<T> fireImmune() { fireImmune = true; return this; }
        public Builder<T> immuneTo(net.minecraft.world.level.block.Block... blocks) { return this; }
        public Builder<T> canSpawnFarFromPlayer() { return this; }
        public Builder<T> clientTrackingRange(int r) { trackingRange = r; return this; }
        public Builder<T> updateInterval(int i) { updateInterval = i; return this; }
        public Builder<T> requiredFeatures(net.minecraft.world.flag.FeatureFlag... flags) { return this; }

        public EntityType<T> build(String id) { return new EntityType<>(factory, category, width, height, fireImmune, trackingRange, updateInterval); }
    }
}
