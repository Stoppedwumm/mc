package net.minecraft.util;

/** Random numbers (reamc-compat wraps java.util.Random). */
public interface RandomSource {
    double GAUSSIAN_SPREAD_FACTOR = 2.297;

    RandomSource fork();
    void setSeed(long seed);
    int nextInt();
    int nextInt(int bound);
    long nextLong();
    boolean nextBoolean();
    float nextFloat();
    double nextDouble();
    double nextGaussian();

    default int nextIntBetweenInclusive(int min, int max) { return min + nextInt(max - min + 1); }

    default int nextInt(int min, int max) {
        if (min >= max) throw new IllegalArgumentException("bound - origin is non positive");
        return min + nextInt(max - min);
    }

    default double triangle(double center, double spread) { return center + spread * (nextDouble() - nextDouble()); }

    default void consumeCount(int n) { for (int i = 0; i < n; i++) nextInt(); }

    static RandomSource create() { return wrap(new java.util.Random()); }

    static RandomSource create(long seed) { return wrap(new java.util.Random(seed)); }

    static RandomSource createThreadSafe() { return create(); }

    static RandomSource createNewThreadLocalInstance() { return create(); }

    static RandomSource wrap(java.util.Random r) {
        return new RandomSource() {
            public RandomSource fork() { return create(r.nextLong()); }
            public void setSeed(long seed) { r.setSeed(seed); }
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
