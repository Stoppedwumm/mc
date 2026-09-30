package mc.world.gen;

import java.util.Random;

/** Ken Perlin's improved noise with random offsets, plus fractal octave sums. */
public final class Noise {
    private final int[] p = new int[512];
    private final double ox, oy, oz;

    public Noise(Random random) {
        ox = random.nextDouble() * 256;
        oy = random.nextDouble() * 256;
        oz = random.nextDouble() * 256;
        int[] perm = new int[256];
        for (int i = 0; i < 256; i++) perm[i] = i;
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256 - i) + i;
            int t = perm[i]; perm[i] = perm[j]; perm[j] = t;
            p[i] = p[i + 256] = perm[i];
        }
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    /** Returns roughly [-1, 1]. */
    public double sample(double x, double y, double z) {
        x += ox; y += oy; z += oz;
        double fx = Math.floor(x), fy = Math.floor(y), fz = Math.floor(z);
        int X = (int) fx & 255, Y = (int) fy & 255, Z = (int) fz & 255;
        x -= fx; y -= fy; z -= fz;
        double u = fade(x), v = fade(y), w = fade(z);
        int A = p[X] + Y, AA = p[A] + Z, AB = p[A + 1] + Z;
        int B = p[X + 1] + Y, BA = p[B] + Z, BB = p[B + 1] + Z;
        return lerp(w,
                lerp(v, lerp(u, grad(p[AA], x, y, z), grad(p[BA], x - 1, y, z)),
                        lerp(u, grad(p[AB], x, y - 1, z), grad(p[BB], x - 1, y - 1, z))),
                lerp(v, lerp(u, grad(p[AA + 1], x, y, z - 1), grad(p[BA + 1], x - 1, y, z - 1)),
                        lerp(u, grad(p[AB + 1], x, y - 1, z - 1), grad(p[BB + 1], x - 1, y - 1, z - 1))));
    }

    /** Fractal sum of several independent noise layers. */
    public static final class Octaves {
        private final Noise[] layers;
        private final double norm;

        public Octaves(Random random, int count) {
            layers = new Noise[count];
            double n = 0, amp = 1;
            for (int i = 0; i < count; i++) {
                layers[i] = new Noise(random);
                n += amp;
                amp *= 0.5;
            }
            norm = 1.0 / n;
        }

        public double sample(double x, double y, double z) {
            double sum = 0, amp = 1, freq = 1;
            for (Noise layer : layers) {
                sum += layer.sample(x * freq, y * freq, z * freq) * amp;
                amp *= 0.5;
                freq *= 2;
            }
            return sum * norm;
        }

        public double sample2(double x, double z) {
            return sample(x, 0.5, z);
        }

        /** Ridged multifractal, 0..1 with sharp crests. */
        public double ridged(double x, double z) {
            double sum = 0, amp = 1, freq = 1;
            for (Noise layer : layers) {
                double n = 1 - Math.abs(layer.sample(x * freq, 0.5, z * freq));
                sum += n * n * amp;
                amp *= 0.5;
                freq *= 2;
            }
            return sum * norm;
        }
    }
}
