package net.minecraft.world.item.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** All recipes loaded from data, by type (reamc-compat: filled by reamc's mod loader). */
public class RecipeManager {
    private final Map<ResourceLocation, RecipeHolder<?>> byId = new LinkedHashMap<>();
    private final Map<RecipeType<?>, List<RecipeHolder<?>>> byType = new LinkedHashMap<>();

    public void reamc$add(RecipeHolder<?> h) {
        RecipeHolder<?> old = byId.put(h.id(), h);
        if (old != null) byType.getOrDefault(old.value().getType(), new ArrayList<>()).remove(old);
        byType.computeIfAbsent(h.value().getType(), k -> new ArrayList<>()).add(h);
    }

    @SuppressWarnings("unchecked")
    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type, I input, Level level) {
        for (RecipeHolder<?> h : byType.getOrDefault(type, List.of())) {
            try {
                if (((Recipe<I>) h.value()).matches(input, level)) return Optional.of((RecipeHolder<T>) h);
            } catch (ClassCastException ignored) { }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type, I input, Level level, ResourceLocation last) {
        if (last != null) {
            RecipeHolder<?> h = byId.get(last);
            if (h != null && h.value().getType() == type && ((Recipe<I>) h.value()).matches(input, level)) return Optional.of((RecipeHolder<T>) h);
        }
        return getRecipeFor(type, input, level);
    }

    @SuppressWarnings("unchecked")
    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type, I input, Level level, RecipeHolder<T> last) {
        return getRecipeFor(type, input, level, last == null ? null : last.id());
    }

    @SuppressWarnings("unchecked")
    public <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> getRecipesFor(RecipeType<T> type, I input, Level level) {
        List<RecipeHolder<T>> out = new ArrayList<>();
        for (RecipeHolder<?> h : byType.getOrDefault(type, List.of())) if (((Recipe<I>) h.value()).matches(input, level)) out.add((RecipeHolder<T>) h);
        return out;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> getAllRecipesFor(RecipeType<T> type) { return (List) List.copyOf(byType.getOrDefault(type, List.of())); }

    public Optional<RecipeHolder<?>> byKey(ResourceLocation id) { return Optional.ofNullable(byId.get(id)); }

    public Collection<RecipeHolder<?>> getRecipes() { return byId.values(); }

    public java.util.stream.Stream<ResourceLocation> getRecipeIds() { return byId.keySet().stream(); }

    public static <I extends RecipeInput, T extends Recipe<I>> CachedCheck<I, T> createCheck(RecipeType<T> type) {
        return new CachedCheck<>() {
            private ResourceLocation last;

            @Override
            public Optional<RecipeHolder<T>> getRecipeFor(I input, Level level) {
                Optional<RecipeHolder<T>> r = level.getRecipeManager().getRecipeFor(type, input, level, last);
                r.ifPresent(h -> last = h.id());
                return r;
            }
        };
    }

    public interface CachedCheck<I extends RecipeInput, T extends Recipe<I>> {
        Optional<RecipeHolder<T>> getRecipeFor(I input, Level level);
    }
}
