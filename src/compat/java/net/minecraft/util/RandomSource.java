package net.minecraft.util;

/** Random numbers (reamc-compat wraps java.util.Random). */
public interface RandomSource {
    int nextInt();
    int nextInt(int bound);
    long nextLong();
    boolean nextBoolean();
    float nextFloat();
    double nextDouble();
    double nextGaussian();

    default int nextIntBetweenInclusive(int min, int max) { return min + nextInt(max - min + 1); }

    static RandomSource create() { return wrap(new java.util.Random()); }

    static RandomSource create(long seed) { return wrap(new java.util.Random(seed)); }

    static RandomSource wrap(java.util.Random r) {
        return new RandomSource() {
            public int nextInt() { return r.nextInt(); }
            public int nextInt(int bound) { return r.nextInt(bound); }
            public long nextLong() { return r.nextLong(); }
            public boolean nextBoolean() { return r.nextBoolean(); }
            public float nextFloat() { return r.nextFloat(); }
            public double nextDouble() { return r.nextDouble(); }
            public double nextGaussian() { return r.nextGaussian(); }
        };
    }
}
