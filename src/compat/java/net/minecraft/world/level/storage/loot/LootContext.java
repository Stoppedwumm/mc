package net.minecraft.world.level.storage.loot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

import java.util.Optional;

/** One loot roll in progress (reamc-compat). */
public class LootContext {
    private final LootParams params;
    private final RandomSource random;
    private final ResourceLocation queriedTable;

    LootContext(LootParams params, RandomSource random, ResourceLocation queriedTable) {
        this.params = params;
        this.random = random;
        this.queriedTable = queriedTable;
    }

    public boolean hasParam(LootContextParam<?> p) { return params.hasParam(p); }
    public <T> T getParam(LootContextParam<T> p) { return params.getParameter(p); }
    public <T> T getParamOrNull(LootContextParam<T> p) { return params.getParamOrNull(p); }
    public RandomSource getRandom() { return random; }
    public float getLuck() { return params.getLuck(); }
    public ServerLevel getLevel() { return params.getLevel(); }
    public LootParams reamc$params() { return params; }
    public ResourceLocation getQueriedLootTableId() { return queriedTable; }
    public void setQueriedLootTableId(ResourceLocation id) { }

    public static class Builder {
        private final LootParams params;
        private RandomSource random;

        public Builder(LootParams params) { this.params = params; }

        public Builder withOptionalRandomSeed(long seed) { if (seed != 0) random = RandomSource.create(seed); return this; }
        public Builder withOptionalRandomSource(RandomSource r) { random = r; return this; }
        public ServerLevel getLevel() { return params.getLevel(); }
        public LootContext create(Optional<ResourceLocation> table) { return new LootContext(params, random != null ? random : RandomSource.create(), table.orElse(null)); }
    }

    public enum EntityTarget { THIS, ATTACKER, DIRECT_ATTACKER, ATTACKING_PLAYER }
}
