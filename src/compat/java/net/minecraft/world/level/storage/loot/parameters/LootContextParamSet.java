package net.minecraft.world.level.storage.loot.parameters;

import java.util.LinkedHashSet;
import java.util.Set;

/** Which parameters a kind of loot context has (reamc-compat). */
public class LootContextParamSet {
    private final Set<LootContextParam<?>> required, all;

    LootContextParamSet(Set<LootContextParam<?>> required, Set<LootContextParam<?>> optional) {
        this.required = Set.copyOf(required);
        Set<LootContextParam<?>> a = new LinkedHashSet<>(required);
        a.addAll(optional);
        this.all = Set.copyOf(a);
    }

    public boolean isAllowed(LootContextParam<?> p) { return all.contains(p); }
    public Set<LootContextParam<?>> getRequired() { return required; }
    public Set<LootContextParam<?>> getAllowed() { return all; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Set<LootContextParam<?>> required = new LinkedHashSet<>(), optional = new LinkedHashSet<>();

        public Builder required(LootContextParam<?> p) { required.add(p); return this; }
        public Builder optional(LootContextParam<?> p) { optional.add(p); return this; }
        public LootContextParamSet build() { return new LootContextParamSet(required, optional); }
    }
}
