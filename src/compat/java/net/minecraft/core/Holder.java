package net.minecraft.core;

import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** A reference to a registry value, by key once registered (reamc-compat). */
public interface Holder<T> {
    T value();

    boolean isBound();

    boolean is(ResourceLocation id);

    boolean is(ResourceKey<T> key);

    boolean is(Predicate<ResourceKey<T>> predicate);

    boolean is(TagKey<T> tag);

    @Deprecated
    boolean is(Holder<T> other);

    Stream<TagKey<T>> tags();

    Either<ResourceKey<T>, T> unwrap();

    Optional<ResourceKey<T>> unwrapKey();

    Kind kind();

    boolean canSerializeIn(HolderOwner<T> owner);

    default String getRegisteredName() { return unwrapKey().map(k -> k.location().toString()).orElse("[unregistered]"); }

    static <T> Holder<T> direct(T value) { return new Direct<>(value); }

    enum Kind { REFERENCE, DIRECT }

    record Direct<T>(T value) implements Holder<T> {
        @Override public boolean isBound() { return true; }
        @Override public boolean is(ResourceLocation id) { return false; }
        @Override public boolean is(ResourceKey<T> key) { return false; }
        @Override public boolean is(TagKey<T> tag) { return false; }
        @Override public boolean is(Holder<T> other) { return value.equals(other.value()); }
        @Override public boolean is(Predicate<ResourceKey<T>> predicate) { return false; }
        @Override public Either<ResourceKey<T>, T> unwrap() { return Either.right(value); }
        @Override public Optional<ResourceKey<T>> unwrapKey() { return Optional.empty(); }
        @Override public Kind kind() { return Kind.DIRECT; }
        @Override public Stream<TagKey<T>> tags() { return Stream.of(); }
        @Override public boolean canSerializeIn(HolderOwner<T> owner) { return true; }
        @Override public String toString() { return "Direct{" + value + "}"; }
    }

    /** A registered entry: its key, its value once bound, and its tags (looked up in reamc's tag data). */
    class Reference<T> implements Holder<T> {
        private final HolderOwner<T> owner;
        private ResourceKey<T> key;
        private T value;

        protected Reference(Type type, HolderOwner<T> owner, ResourceKey<T> key, T value) {
            this.owner = owner;
            this.key = key;
            this.value = value;
        }

        public static <T> Reference<T> createStandAlone(HolderOwner<T> owner, ResourceKey<T> key) { return new Reference<>(Type.STAND_ALONE, owner, key, null); }

        public static <T> Reference<T> createIntrusive(HolderOwner<T> owner, T value) { return new Reference<>(Type.INTRUSIVE, owner, null, value); }

        public ResourceKey<T> key() {
            if (key == null) throw new IllegalStateException("Trying to access unbound value '" + value + "' from registry " + owner);
            return key;
        }

        @Override
        public T value() {
            if (value == null) throw new IllegalStateException("Trying to access unbound value '" + key + "' from registry " + owner);
            return value;
        }

        @Override public boolean is(ResourceLocation id) { return key().location().equals(id); }
        @Override public boolean is(ResourceKey<T> k) { return key() == k || key().equals(k); }
        @Override public boolean is(TagKey<T> tag) { return key != null && mc.mod.Tags.has(tag, key.location()); }
        @Override public boolean is(Holder<T> other) { return other.is(key()); }
        @Override public boolean is(Predicate<ResourceKey<T>> predicate) { return predicate.test(key()); }
        @Override public boolean canSerializeIn(HolderOwner<T> o) { return owner.canSerializeIn(o); }
        @Override public Either<ResourceKey<T>, T> unwrap() { return Either.left(key()); }
        @Override public Optional<ResourceKey<T>> unwrapKey() { return Optional.ofNullable(key); }
        @Override public Kind kind() { return Kind.REFERENCE; }
        @Override public boolean isBound() { return key != null && value != null; }
        @Override public Stream<TagKey<T>> tags() { return key == null ? Stream.of() : mc.mod.Tags.tagsOf(key); }

        void bindKey(ResourceKey<T> k) { key = k; }

        protected void bindValue(T v) { value = v; }

        @Override public String toString() { return "Reference{" + key + "=" + value + "}"; }

        protected enum Type { STAND_ALONE, INTRUSIVE }
    }
}
