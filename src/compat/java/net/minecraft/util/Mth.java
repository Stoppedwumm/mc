package net.minecraft.util;

import net.minecraft.core.Vec3i;

/** Minecraft's math helpers (reamc-compat). */
public class Mth {
    public static final float PI = (float) Math.PI, HALF_PI = PI / 2, TWO_PI = PI * 2, DEG_TO_RAD = PI / 180, RAD_TO_DEG = 180 / PI, EPSILON = 1.0E-5f;
    public static final float SQRT_OF_TWO = (float) Math.sqrt(2);

    public Mth() { }

    public static float sin(float v) { return (float) Math.sin(v); }
    public static float cos(float v) { return (float) Math.cos(v); }
    public static float sqrt(float v) { return (float) Math.sqrt(v); }
    public static int floor(float v) { return (int) Math.floor(v); }
    public static int floor(double v) { return (int) Math.floor(v); }
    public static long lfloor(double v) { return (long) Math.floor(v); }
    public static float abs(float v) { return Math.abs(v); }
    public static int abs(int v) { return Math.abs(v); }
    public static int ceil(float v) { return (int) Math.ceil(v); }
    public static int ceil(double v) { return (int) Math.ceil(v); }
    public static int clamp(int v, int min, int max) { return Math.min(Math.max(v, min), max); }
    public static long clamp(long v, long min, long max) { return Math.min(Math.max(v, min), max); }
    public static float clamp(float v, float min, float max) { return v < min ? min : Math.min(v, max); }
    public static double clamp(double v, double min, double max) { return v < min ? min : Math.min(v, max); }
    public static double clampedLerp(double a, double b, double t) { return t < 0 ? a : t > 1 ? b : lerp(t, a, b); }
    public static float clampedLerp(float a, float b, float t) { return t < 0 ? a : t > 1 ? b : lerp(t, a, b); }
    public static double absMax(double a, double b) { return Math.max(Math.abs(a), Math.abs(b)); }
    public static int floorDiv(int a, int b) { return Math.floorDiv(a, b); }
    public static int nextInt(RandomSource r, int min, int max) { return min >= max ? min : r.nextInt(max - min + 1) + min; }
    public static float nextFloat(RandomSource r, float min, float max) { return min >= max ? min : r.nextFloat() * (max - min) + min; }
    public static double nextDouble(RandomSource r, double min, double max) { return min >= max ? min : r.nextDouble() * (max - min) + min; }
    public static boolean equal(float a, float b) { return Math.abs(b - a) < EPSILON; }
    public static boolean equal(double a, double b) { return Math.abs(b - a) < EPSILON; }
    public static int positiveModulo(int a, int b) { return Math.floorMod(a, b); }
    public static float positiveModulo(float a, float b) { return (a % b + b) % b; }
    public static double positiveModulo(double a, double b) { return (a % b + b) % b; }
    public static boolean isMultipleOf(int a, int b) { return a % b == 0; }
    public static int wrapDegrees(int d) { int i = d % 360; if (i >= 180) i -= 360; if (i < -180) i += 360; return i; }
    public static float wrapDegrees(float d) { float f = d % 360; if (f >= 180) f -= 360; if (f < -180) f += 360; return f; }
    public static double wrapDegrees(double d) { double f = d % 360; if (f >= 180) f -= 360; if (f < -180) f += 360; return f; }
    public static float degreesDifference(float a, float b) { return wrapDegrees(b - a); }
    public static float degreesDifferenceAbs(float a, float b) { return abs(degreesDifference(a, b)); }
    public static float approach(float from, float to, float step) { step = abs(step); return from < to ? clamp(from + step, from, to) : clamp(from - step, to, from); }
    public static float approachDegrees(float from, float to, float step) { return approach(from, from + degreesDifference(from, to), step); }
    public static int smallestEncompassingPowerOfTwo(int v) { int i = v - 1; i |= i >> 1; i |= i >> 2; i |= i >> 4; i |= i >> 8; i |= i >> 16; return i + 1; }
    public static boolean isPowerOfTwo(int v) { return v != 0 && (v & v - 1) == 0; }
    public static int ceillog2(int v) { v = isPowerOfTwo(v) ? v : smallestEncompassingPowerOfTwo(v); return Integer.numberOfTrailingZeros(v); }
    public static int log2(int v) { return ceillog2(v) - (isPowerOfTwo(v) ? 0 : 1); }
    public static int color(float r, float g, float b) { return color(floor(r * 255), floor(g * 255), floor(b * 255)); }
    public static int color(int r, int g, int b) { return (r << 8 | g) << 8 | b; }
    public static float frac(float v) { return v - floor(v); }
    public static double frac(double v) { return v - lfloor(v); }
    public static long getSeed(Vec3i p) { return getSeed(p.getX(), p.getY(), p.getZ()); }
    public static long getSeed(int x, int y, int z) { long l = (long) (x * 3129871) ^ (long) z * 116129781L ^ (long) y; l = l * l * 42317861L + l * 11L; return l >> 16; }
    public static java.util.UUID createInsecureUUID(RandomSource r) { return new java.util.UUID(r.nextLong() & -61441L | 16384L, r.nextLong() & 4611686018427387903L | Long.MIN_VALUE); }
    public static java.util.UUID createInsecureUUID() { return java.util.UUID.randomUUID(); }
    public static double inverseLerp(double v, double a, double b) { return (v - a) / (b - a); }
    public static float inverseLerp(float v, float a, float b) { return (v - a) / (b - a); }
    public static double atan2(double y, double x) { return Math.atan2(y, x); }
    public static float invSqrt(float v) { return 1 / (float) Math.sqrt(v); }
    public static double invSqrt(double v) { return 1 / Math.sqrt(v); }
    public static int hsvToRgb(float h, float s, float v) { return java.awt.Color.HSBtoRGB(h, s, v) & 0xFFFFFF; }
    public static int hsvToArgb(float h, float s, float v, int a) { return a << 24 | hsvToRgb(h, s, v); }
    public static int murmurHash3Mixer(int v) { v ^= v >>> 16; v *= -2048144789; v ^= v >>> 13; v *= -1028477387; return v ^ v >>> 16; }
    public static int binarySearch(int min, int max, java.util.function.IntPredicate p) {
        int n = max - min;
        while (n > 0) { int h = n / 2, m = min + h; if (p.test(m)) n = h; else { min = m + 1; n -= h + 1; } }
        return min;
    }
    public static int lerpInt(float t, int a, int b) { return a + floor(t * (b - a)); }
    public static int lerpDiscrete(float t, int a, int b) { int d = b - a; return a + floor(t * (d - 1)) + (t > 0 ? 1 : 0); }
    public static float lerp(float t, float a, float b) { return a + t * (b - a); }
    public static double lerp(double t, double a, double b) { return a + t * (b - a); }
    public static double smoothstep(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    public static int sign(double v) { return v == 0 ? 0 : v > 0 ? 1 : -1; }
    public static float rotLerp(float t, float a, float b) { return a + t * wrapDegrees(b - a); }
    public static double rotLerp(double t, double a, double b) { return a + t * wrapDegrees(b - a); }
    public static float triangleWave(float x, float period) { return (Math.abs(x % period - period * 0.5f) - period * 0.25f) / (period * 0.25f); }
    public static float square(float v) { return v * v; }
    public static double square(double v) { return v * v; }
    public static int square(int v) { return v * v; }
    public static long square(long v) { return v * v; }
    public static double clampedMap(double v, double a, double b, double c, double d) { return clampedLerp(c, d, inverseLerp(v, a, b)); }
    public static float clampedMap(float v, float a, float b, float c, float d) { return clampedLerp(c, d, inverseLerp(v, a, b)); }
    public static double map(double v, double a, double b, double c, double d) { return lerp(inverseLerp(v, a, b), c, d); }
    public static float map(float v, float a, float b, float c, float d) { return lerp(inverseLerp(v, a, b), c, d); }
    public static int roundToward(int v, int m) { return positiveCeilDiv(v, m) * m; }
    public static int positiveCeilDiv(int a, int b) { return -Math.floorDiv(-a, b); }
    public static int quantize(double v, int step) { return floor(v / step) * step; }
}
