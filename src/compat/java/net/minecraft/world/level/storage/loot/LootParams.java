package net.minecraft.world.level.storage.loot;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;

import java.util.IdentityHashMap;
import java.util.Map;

/** The facts a loot roll knows: block, tool, entity... (reamc-compat). */
public class LootParams {
    private final ServerLevel level;
    private final Map<LootContextParam<?>, Object> params;
    private final float luck;

    LootParams(ServerLevel level, Map<LootContextParam<?>, Object> params, float luck) { this.level = level; this.params = params; this.luck = luck; }

    public ServerLevel getLevel() { return level; }
    public float getLuck() { return luck; }
    public boolean hasParam(LootContextParam<?> p) { return params.containsKey(p); }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(LootContextParam<T> p) {
        T v = (T) params.get(p);
        if (v == null) throw new java.util.NoSuchElementException(p.toString());
        return v;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOptionalParameter(LootContextParam<T> p) { return (T) params.get(p); }

    @SuppressWarnings("unchecked")
    public <T> T getParamOrNull(LootContextParam<T> p) { return (T) params.get(p); }

    public static class Builder {
        private final ServerLevel level;
        private final Map<LootContextParam<?>, Object> params = new IdentityHashMap<>();
        private float luck;

        public Builder(ServerLevel level) { this.level = level; }

        public ServerLevel getLevel() { return level; }
        public <T> Builder withParameter(LootContextParam<T> p, T v) { params.put(p, v); return this; }
        public <T> Builder withOptionalParameter(LootContextParam<T> p, T v) { if (v == null) params.remove(p); else params.put(p, v); return this; }
        public <T> T getParameter(LootContextParam<T> p) { return getOptionalParameter(p); }
        @SuppressWarnings("unchecked") public <T> T getOptionalParameter(LootContextParam<T> p) { return (T) params.get(p); }
        public Builder withLuck(float l) { luck = l; return this; }
        public LootParams create(LootContextParamSet set) { return new LootParams(level, new IdentityHashMap<>(params), luck); }
    }
}
