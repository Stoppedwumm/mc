package net.minecraft.core;

import com.mojang.datafixers.util.Either;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/** A set of registry entries: listed directly or named by a tag (reamc-compat). */
public interface HolderSet<T> extends Iterable<Holder<T>> {
    Stream<Holder<T>> stream();
    int size();
    Either<TagKey<T>, List<Holder<T>>> unwrap();
    Optional<Holder<T>> getRandomElement(RandomSource random);
    Holder<T> get(int index);
    boolean contains(Holder<T> holder);
    boolean canSerializeIn(HolderOwner<T> owner);
    Optional<TagKey<T>> unwrapKey();

    @SafeVarargs
    static <T> Direct<T> direct(Holder<T>... holders) { return new Direct<>(List.of(holders)); }

    static <T> Direct<T> direct(List<? extends Holder<T>> holders) { return new Direct<>(List.copyOf(holders)); }

    @SafeVarargs
    static <E, T> Direct<T> direct(Function<E, Holder<T>> f, E... values) { return direct(Stream.of(values).map(f).toList()); }

    static <E, T> Direct<T> direct(Function<E, Holder<T>> f, java.util.Collection<E> values) { return direct(values.stream().map(f).toList()); }

    static <T> Named<T> emptyNamed(HolderOwner<T> owner, TagKey<T> key) { return new Named<>(owner, key); }

    abstract class ListBacked<T> implements HolderSet<T> {
        protected abstract List<Holder<T>> contents();
        @Override public int size() { return contents().size(); }
        @Override public Iterator<Holder<T>> iterator() { return contents().iterator(); }
        @Override public Stream<Holder<T>> stream() { return contents().stream(); }
        @Override public Optional<Holder<T>> getRandomElement(RandomSource r) { List<Holder<T>> c = contents(); return c.isEmpty() ? Optional.empty() : Optional.of(c.get(r.nextInt(c.size()))); }
        @Override public Holder<T> get(int index) { return contents().get(index); }
        @Override public boolean canSerializeIn(HolderOwner<T> owner) { return true; }
    }

    final class Direct<T> extends ListBacked<T> {
        private final List<Holder<T>> contents;

        Direct(List<Holder<T>> contents) { this.contents = contents; }

        @Override protected List<Holder<T>> contents() { return contents; }
        @Override public Either<TagKey<T>, List<Holder<T>>> unwrap() { return Either.right(contents); }
        @Override public Optional<TagKey<T>> unwrapKey() { return Optional.empty(); }
        @Override public boolean contains(Holder<T> h) { for (Holder<T> c : contents) if (c == h || c.unwrapKey().isPresent() && c.unwrapKey().equals(h.unwrapKey()) || c.value() == h.value()) return true; return false; }
        @Override public String toString() { return "DirectSet" + contents; }
    }

    /** The entries of a tag, resolved from reamc's tag data each time it is asked. */
    class Named<T> extends ListBacked<T> {
        private final HolderOwner<T> owner;
        private final TagKey<T> key;

        Named(HolderOwner<T> owner, TagKey<T> key) { this.owner = owner; this.key = key; }

        public TagKey<T> key() { return key; }

        @Override
        @SuppressWarnings("unchecked")
        protected List<Holder<T>> contents() {
            Registry<T> r = (Registry<T>) net.minecraft.core.registries.BuiltInRegistries.REGISTRY.get(key.registry().location());
            if (r == null) return List.of();
            return mc.mod.Tags.entries(key).stream().map(id -> r.getHolder(id)).filter(Optional::isPresent).map(o -> (Holder<T>) o.get()).toList();
        }

        @Override public Either<TagKey<T>, List<Holder<T>>> unwrap() { return Either.left(key); }
        @Override public Optional<TagKey<T>> unwrapKey() { return Optional.of(key); }
        @Override public boolean contains(Holder<T> h) { return h.is(key); }
        @Override public boolean canSerializeIn(HolderOwner<T> o) { return owner.canSerializeIn(o); }
        @Override public String toString() { return "NamedSet(" + key + ")" + contents(); }
    }
}
