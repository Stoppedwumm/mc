package net.minecraft.network.codec;

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
        return of((b, v) -> { }, b -> value);
    }

    @FunctionalInterface
    interface StreamMemberEncoder<O, T> {
        void encode(T value, O buffer);
    }
}
