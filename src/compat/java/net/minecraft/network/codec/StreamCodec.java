package net.minecraft.network.codec;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/** Reads and writes a value to a buffer (reamc-compat). */
public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {
    static <B, V> StreamCodec<B, V> of(StreamEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return new StreamCodec<>() {
            @Override public V decode(B buffer) { return decoder.decode(buffer); }
            @Override public void encode(B buffer, V value) { encoder.encode(buffer, value); }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(StreamMemberEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return of((b, v) -> encoder.encode(v, b), decoder);
    }

    static <B, V> StreamCodec<B, V> unit(V value) {
        return of((b, v) -> {
            if (!v.equals(value)) throw new IllegalStateException("Can't encode '" + v + "', expected '" + value + "'");
        }, b -> value);
    }

    default <O> StreamCodec<B, O> apply(CodecOperation<B, V, O> op) { return op.apply(this); }

    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from) {
        StreamCodec<B, V> self = this;
        return new StreamCodec<>() {
            @Override public O decode(B b) { return to.apply(self.decode(b)); }
            @Override public void encode(B b, O v) { self.encode(b, from.apply(v)); }
        };
    }

    default <O extends io.netty.buffer.ByteBuf> StreamCodec<O, V> mapStream(Function<O, ? extends B> f) {
        StreamCodec<B, V> self = this;
        return new StreamCodec<>() {
            @Override public V decode(O b) { return self.decode(f.apply(b)); }
            @Override public void encode(O b, V v) { self.encode(f.apply(b), v); }
        };
    }

    default <U> StreamCodec<B, U> dispatch(Function<? super U, ? extends V> typeOf, Function<? super V, ? extends StreamCodec<? super B, ? extends U>> codecFor) {
        StreamCodec<B, V> self = this;
        return new StreamCodec<>() {
            @Override public U decode(B b) { return codecFor.apply(self.decode(b)).decode(b); }

            @Override
            @SuppressWarnings("unchecked")
            public void encode(B b, U v) {
                V type = typeOf.apply(v);
                self.encode(b, type);
                ((StreamCodec<B, U>) codecFor.apply(type)).encode(b, v);
            }
        };
    }

    @SuppressWarnings("unchecked")
    default <S extends B> StreamCodec<S, V> cast() { return (StreamCodec<S, V>) this; }

    static <B, T> StreamCodec<B, T> recursive(UnaryOperator<StreamCodec<B, T>> f) {
        @SuppressWarnings("unchecked")
        StreamCodec<B, T>[] ref = new StreamCodec[1];
        StreamCodec<B, T> lazy = new StreamCodec<>() {
            @Override public T decode(B b) { return ref[0].decode(b); }
            @Override public void encode(B b, T v) { ref[0].encode(b, v); }
        };
        ref[0] = f.apply(lazy);
        return ref[0];
    }

    static <B, C, T1> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, Function<T1, C> factory) {
        return new StreamCodec<>() {
            @Override
            public C decode(B b) {
                T1 v1 = c1.decode(b);
                return factory.apply(v1);
            }

            @Override
            public void encode(B b, C v) {
                c1.encode(b, g1.apply(v));
            }
        };
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, BiFunction<T1, T2, C> factory) {
        return new StreamCodec<>() {
            @Override
            public C decode(B b) {
                T1 v1 = c1.decode(b);
                T2 v2 = c2.decode(b);
                return factory.apply(v1, v2);
            }

            @Override
            public void encode(B b, C v) {
                c1.encode(b, g1.apply(v));
                c2.encode(b, g2.apply(v));
            }
        };
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3, com.mojang.datafixers.util.Function3<T1, T2, T3, C> factory) {
        return new StreamCodec<>() {
            @Override
            public C decode(B b) {
                T1 v1 = c1.decode(b);
                T2 v2 = c2.decode(b);
                T3 v3 = c3.decode(b);
                return factory.apply(v1, v2, v3);
            }

            @Override
            public void encode(B b, C v) {
                c1.encode(b, g1.apply(v));
                c2.encode(b, g2.apply(v));
                c3.encode(b, g3.apply(v));
            }
        };
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4, com.mojang.datafixers.util.Function4<T1, T2, T3, T4, C> factory) {
        return new StreamCodec<>() {
            @Override
            public C decode(B b) {
                T1 v1 = c1.decode(b);
                T2 v2 = c2.decode(b);
                T3 v3 = c3.decode(b);
                T4 v4 = c4.decode(b);
                return factory.apply(v1, v2, v3, v4);
            }

            @Override
            public void encode(B b, C v) {
                c1.encode(b, g1.apply(v));
                c2.encode(b, g2.apply(v));
                c3.encode(b, g3.apply(v));
                c4.encode(b, g4.apply(v));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4, StreamCodec<? super B, T5> c5, Function<C, T5> g5, com.mojang.datafixers.util.Function5<T1, T2, T3, T4, T5, C> factory) {
        return new StreamCodec<>() {
            @Override
            public C decode(B b) {
                T1 v1 = c1.decode(b);
                T2 v2 = c2.decode(b);
                T3 v3 = c3.decode(b);
                T4 v4 = c4.decode(b);
                T5 v5 = c5.decode(b);
                return factory.apply(v1, v2, v3, v4, v5);
            }

            @Override
            public void encode(B b, C v) {
                c1.encode(b, g1.apply(v));
                c2.encode(b, g2.apply(v));
                c3.encode(b, g3.apply(v));
                c4.encode(b, g4.apply(v));
                c5.encode(b, g5.apply(v));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4, StreamCodec<? super B, T5> c5, Function<C, T5> g5, StreamCodec<? super B, T6> c6, Function<C, T6> g6, com.mojang.datafixers.util.Function6<T1, T2, T3, T4, T5, T6, C> factory) {
        return new StreamCodec<>() {
            @Override
            public C decode(B b) {
                T1 v1 = c1.decode(b);
                T2 v2 = c2.decode(b);
                T3 v3 = c3.decode(b);
                T4 v4 = c4.decode(b);
                T5 v5 = c5.decode(b);
                T6 v6 = c6.decode(b);
                return factory.apply(v1, v2, v3, v4, v5, v6);
            }

            @Override
            public void encode(B b, C v) {
                c1.encode(b, g1.apply(v));
                c2.encode(b, g2.apply(v));
                c3.encode(b, g3.apply(v));
                c4.encode(b, g4.apply(v));
                c5.encode(b, g5.apply(v));
                c6.encode(b, g6.apply(v));
            }
        };
    }

    @FunctionalInterface
    interface StreamMemberEncoder<O, T> {
        void encode(T value, O buffer);
    }

    @FunctionalInterface
    interface CodecOperation<B, S, T> {
        StreamCodec<B, T> apply(StreamCodec<B, S> codec);
    }
}
