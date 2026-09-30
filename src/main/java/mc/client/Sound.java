package mc.client;

import mc.world.Block;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryUtil;

import java.nio.ShortBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

/**
 * OpenAL sound engine (same API Minecraft uses). All sounds are synthesised at startup from filtered noise,
 * so no audio assets are needed. Fails silently when no audio device is available.
 */
public final class Sound {
    private static final int RATE = 22050;
    private long device, context;
    private boolean ok;
    private final int[] sources = new int[24];
    private int nextSource;
    private final Map<Block.SoundType, int[]> dig = new EnumMap<>(Block.SoundType.class);
    private final Map<Block.SoundType, int[]> step = new EnumMap<>(Block.SoundType.class);
    private int splash, click;
    private final java.util.Map<String, int[]> named = new java.util.HashMap<>();
    private int rainSource, rainBuffer;
    private final Random random = new Random();
    public float volume = 1f;

    public Sound() {
        try {
            device = alcOpenDevice((CharSequence) null);
            if (device == 0) throw new IllegalStateException("No audio device");
            ALCCapabilities caps = ALC.createCapabilities(device);
            context = alcCreateContext(device, (int[]) null);
            alcMakeContextCurrent(context);
            AL.createCapabilities(caps);
            for (int i = 0; i < sources.length; i++) sources[i] = alGenSources();
            alDistanceModel(AL_INVERSE_DISTANCE_CLAMPED);
            for (Block.SoundType t : Block.SoundType.values()) {
                if (t == Block.SoundType.NONE) continue;
                int[] d = new int[3], s = new int[3];
                for (int v = 0; v < 3; v++) {
                    d[v] = buffer(synth(t, 0.22f, v));
                    s[v] = buffer(synth(t, 0.12f, v + 10));
                }
                dig.put(t, d);
                step.put(t, s);
            }
            splash = buffer(splashSamples());
            click = buffer(clickSamples());
            buildNamed();
            rainBuffer = buffer(rainSamples());
            rainSource = alGenSources();
            alSourcei(rainSource, AL_BUFFER, rainBuffer);
            alSourcei(rainSource, AL_LOOPING, AL_TRUE);
            alSourcei(rainSource, AL_SOURCE_RELATIVE, AL_TRUE);
            alSourcef(rainSource, AL_GAIN, 0);
            alSourcePlay(rainSource);
            ok = true;
        } catch (Throwable e) {
            System.err.println("Sound disabled: " + e.getMessage());
            ok = false;
        }
    }

    private int buffer(short[] samples) {
        int b = alGenBuffers();
        ShortBuffer sb = MemoryUtil.memAllocShort(samples.length);
        sb.put(samples).flip();
        alBufferData(b, AL_FORMAT_MONO16, sb, RATE);
        MemoryUtil.memFree(sb);
        return b;
    }

    /** Material-specific noise burst: filter cutoff, decay and grain differ per material. */
    private short[] synth(Block.SoundType type, float length, int variant) {
        Random r = new Random(type.ordinal() * 100L + variant);
        int n = (int) (RATE * length);
        short[] out = new short[n];
        double cutoff, decay, grains, tone = 0, amp = 0.6;
        switch (type) {
            case STONE -> { cutoff = 0.35; decay = 28; grains = 0.0; tone = 180; }
            case WOOD -> { cutoff = 0.2; decay = 22; grains = 0; tone = 140; amp = 0.8; }
            case GRASS -> { cutoff = 0.25; decay = 14; grains = 0.25; }
            case GRAVEL -> { cutoff = 0.3; decay = 16; grains = 0.5; }
            case SAND -> { cutoff = 0.55; decay = 14; grains = 0.35; amp = 0.4; }
            case GLASS -> { cutoff = 0.8; decay = 20; grains = 0.1; tone = 2400; amp = 0.5; }
            case CLOTH -> { cutoff = 0.08; decay = 18; grains = 0; amp = 0.7; }
            case SNOW -> { cutoff = 0.3; decay = 12; grains = 0.45; amp = 0.5; }
            default -> { cutoff = 0.3; decay = 20; grains = 0; }
        }
        double lp = 0, lp2 = 0;
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            double noise = r.nextDouble() * 2 - 1;
            if (grains > 0 && r.nextDouble() < grains * 0.02) noise *= 4;
            lp += (noise - lp) * cutoff;
            lp2 += (lp - lp2) * cutoff;
            double v = lp2 * 2.5;
            if (tone > 0) {
                phase += 2 * Math.PI * tone * (1 + variant * 0.07) / RATE;
                v = v * 0.7 + Math.sin(phase) * Math.exp(-t * 60) * 0.5;
            }
            double env = Math.exp(-t * decay) * Math.min(1, t * 400);
            out[i] = (short) (Math.max(-1, Math.min(1, v * env * amp)) * 32000);
        }
        return out;
    }

    /**
     * Generic voice synthesiser: a tone (with optional vibrato, tremolo and pitch slide) mixed with filtered
     * noise and rhythmic clicks, under an attack/decay envelope.
     */
    private short[] voice(double freq, double seconds, double noiseMix, double vibrato, double tremolo, double slide,
                          double clicks, double cutoff, long seed) {
        Random r = new Random(seed);
        int n = (int) (RATE * seconds);
        short[] out = new short[n];
        double phase = 0, lp = 0, lp2 = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE, p = t / seconds;
            double f = freq * (1 + slide * p) * (1 + vibrato * Math.sin(t * 2 * Math.PI * 6));
            phase += 2 * Math.PI * f / RATE;
            // Slightly buzzy tone: fundamental plus harmonics
            double tone = Math.sin(phase) * 0.6 + Math.sin(phase * 2) * 0.25 + Math.sin(phase * 3) * 0.12 + Math.signum(Math.sin(phase)) * 0.08;
            double noise = r.nextDouble() * 2 - 1;
            lp += (noise - lp) * cutoff;
            lp2 += (lp - lp2) * cutoff;
            double v = tone * (1 - noiseMix) + lp2 * 3 * noiseMix;
            if (tremolo > 0) v *= 0.6 + 0.4 * Math.sin(t * 2 * Math.PI * tremolo);
            if (clicks > 0) v *= (Math.sin(t * 2 * Math.PI * clicks) > 0.3 ? 1 : 0.15);
            double env = Math.min(1, p * 12) * Math.pow(1 - p, 1.5);
            out[i] = (short) (Math.max(-1, Math.min(1, v * env * 0.8)) * 30000);
        }
        return out;
    }

    private void named(String name, short[]... variants) {
        int[] ids = new int[variants.length];
        for (int i = 0; i < variants.length; i++) ids[i] = buffer(variants[i]);
        named.put(name, ids);
    }

    private void buildNamed() {
        named("pig_say", voice(160, 0.35, 0.35, 0.05, 0, -0.3, 0, 0.2, 1), voice(180, 0.3, 0.35, 0.05, 0, -0.2, 0, 0.2, 2));
        named("pig_hurt", voice(260, 0.25, 0.3, 0.1, 0, -0.4, 0, 0.3, 3));
        named("pig_death", voice(220, 0.6, 0.3, 0.1, 0, -0.6, 0, 0.3, 4));
        named("cow_say", voice(105, 1.1, 0.15, 0.03, 0, -0.15, 0, 0.1, 5), voice(95, 0.9, 0.15, 0.03, 0, 0.1, 0, 0.1, 6));
        named("cow_hurt", voice(150, 0.4, 0.2, 0.05, 0, -0.3, 0, 0.2, 7));
        named("cow_death", voice(120, 0.9, 0.2, 0.05, 0, -0.5, 0, 0.2, 8));
        named("sheep_say", voice(320, 0.7, 0.2, 0.02, 14, -0.1, 0, 0.3, 9), voice(290, 0.6, 0.2, 0.02, 12, 0, 0, 0.3, 10));
        named("sheep_hurt", voice(400, 0.3, 0.2, 0.02, 16, -0.2, 0, 0.3, 11));
        named("sheep_death", voice(350, 0.7, 0.2, 0.02, 12, -0.5, 0, 0.3, 12));
        named("chicken_say", voice(900, 0.25, 0.3, 0.1, 0, 0.3, 18, 0.5, 13), voice(1000, 0.2, 0.3, 0.1, 0, 0.2, 22, 0.5, 14));
        named("chicken_hurt", voice(1200, 0.2, 0.3, 0.1, 0, -0.3, 0, 0.5, 15));
        named("chicken_death", voice(1000, 0.4, 0.3, 0.1, 0, -0.5, 0, 0.5, 16));
        named("zombie_say", voice(85, 1.2, 0.5, 0.08, 3, -0.2, 0, 0.08, 17), voice(75, 1.0, 0.5, 0.1, 2, 0.1, 0, 0.08, 18));
        named("zombie_hurt", voice(120, 0.4, 0.5, 0.1, 0, -0.3, 0, 0.1, 19));
        named("zombie_death", voice(90, 1.2, 0.5, 0.1, 0, -0.6, 0, 0.1, 20));
        named("skeleton_say", voice(600, 0.4, 0.8, 0, 0, 0, 25, 0.6, 21));
        named("skeleton_hurt", voice(700, 0.3, 0.8, 0, 0, 0, 30, 0.6, 22));
        named("skeleton_death", voice(500, 0.7, 0.8, 0, 0, -0.3, 20, 0.6, 23));
        named("spider_say", voice(200, 0.6, 0.9, 0, 0, 0, 0, 0.7, 24));
        named("spider_hurt", voice(300, 0.3, 0.9, 0, 0, 0, 0, 0.7, 25));
        named("spider_death", voice(200, 0.8, 0.9, 0, 0, -0.3, 0, 0.6, 26));
        named("creeper_hurt", voice(300, 0.25, 0.7, 0, 0, 0, 0, 0.4, 27));
        named("creeper_death", voice(250, 0.4, 0.7, 0, 0, -0.2, 0, 0.4, 28));
        named("creeper_say", voice(250, 0.2, 0.9, 0, 0, 0, 0, 0.5, 29));
        named("fuse", voice(3000, 1.4, 1.0, 0, 0, 0, 0, 0.9, 30));
        named("fizz", voice(3000, 0.5, 1.0, 0, 0, 0, 0, 0.9, 31));
        named("explode", explosionSamples(40), explosionSamples(41));
        named("hurt", voice(280, 0.25, 0.25, 0.05, 0, -0.35, 0, 0.3, 32));
        named("hit", voice(150, 0.12, 0.8, 0, 0, 0, 0, 0.3, 33));
        named("pop", voice(700, 0.08, 0.1, 0, 0, 0.8, 0, 0.3, 34));
        named("eat", voice(400, 0.15, 0.9, 0, 0, 0, 30, 0.35, 35), voice(450, 0.15, 0.9, 0, 0, 0, 25, 0.35, 36));
        named("burp", voice(90, 0.4, 0.3, 0.1, 0, -0.2, 0, 0.15, 37));
        named("bow", voice(900, 0.25, 0.8, 0, 0, -0.6, 0, 0.5, 38));
        named("arrow_hit", voice(250, 0.12, 0.7, 0, 0, 0, 0, 0.4, 39));
        named("thunder", explosionSamples(50));
        named("bucket", voice(200, 0.35, 0.8, 0, 8, 0.3, 0, 0.3, 42));
        named("door_open", voice(140, 0.45, 0.75, 0.02, 0, 0.4, 12, 0.25, 43), voice(160, 0.4, 0.75, 0.02, 0, 0.5, 14, 0.25, 44));
        named("door_close", voice(110, 0.25, 0.85, 0, 0, -0.3, 0, 0.35, 45));
        named("armor", voice(700, 0.18, 0.85, 0, 20, -0.2, 0, 0.5, 46));
        named("levelup", chime(new double[]{523, 659, 784, 1046}, 0.9));
        named("orb", chime(new double[]{1320, 1760}, 0.18));
    }

    /** Bell-like arpeggio for level ups and experience pickups. */
    private short[] chime(double[] notes, double seconds) {
        int n = (int) (RATE * seconds);
        short[] out = new short[n];
        double step = seconds / notes.length;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE, v = 0;
            for (int k = 0; k < notes.length; k++) {
                double lt = t - k * step * 0.6;
                if (lt < 0) continue;
                v += Math.sin(2 * Math.PI * notes[k] * lt) * Math.exp(-lt * 6) * 0.35;
            }
            out[i] = (short) (Math.max(-1, Math.min(1, v)) * 26000);
        }
        return out;
    }

    private short[] explosionSamples(long seed) {
        Random r = new Random(seed);
        int n = (int) (RATE * 2.2);
        short[] out = new short[n];
        double lp = 0, lp2 = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            double cutoff = 0.25 * Math.exp(-t * 1.5) + 0.02;
            lp += ((r.nextDouble() * 2 - 1) - lp) * cutoff;
            lp2 += (lp - lp2) * cutoff;
            double env = Math.min(1, t * 200) * Math.exp(-t * 2.2);
            out[i] = (short) (Math.max(-1, Math.min(1, lp2 * 6 * env)) * 30000);
        }
        return out;
    }

    private short[] rainSamples() {
        Random r = new Random(99);
        int n = RATE * 3;
        short[] out = new short[n];
        double lp = 0;
        for (int i = 0; i < n; i++) {
            lp += ((r.nextDouble() * 2 - 1) - lp) * 0.45;
            double drop = r.nextDouble() < 0.003 ? (r.nextDouble() - 0.5) * 2 : 0;
            out[i] = (short) (Math.max(-1, Math.min(1, lp * 0.35 + drop * 0.3)) * 20000);
        }
        return out;
    }

    /** Plays a named sound at a world position (see buildNamed). */
    public void play(String name, double x, double y, double z, float volume, float pitch) {
        int[] set = named.get(name);
        if (set == null) return;
        play(set[random.nextInt(set.length)], Math.min(1, volume), pitch, x, y, z, false);
    }

    public void playUi(String name, float volume, float pitch) {
        int[] set = named.get(name);
        if (set == null) return;
        play(set[random.nextInt(set.length)], volume, pitch, 0, 0, 0, true);
    }

    public void setRain(float level) {
        if (ok) alSourcef(rainSource, AL_GAIN, level * 0.35f * volume);
    }

    private short[] splashSamples() {
        Random r = new Random(7);
        int n = (int) (RATE * 0.6);
        short[] out = new short[n];
        double lp = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            lp += ((r.nextDouble() * 2 - 1) - lp) * 0.3;
            out[i] = (short) (lp * Math.exp(-t * 5) * Math.min(1, t * 100) * 30000);
        }
        return out;
    }

    private short[] clickSamples() {
        int n = RATE / 30;
        short[] out = new short[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            out[i] = (short) (Math.sin(t * 2 * Math.PI * 1200) * Math.exp(-t * 200) * 12000);
        }
        return out;
    }

    private void play(int buffer, float gain, float pitch, double x, double y, double z, boolean relative) {
        if (!ok) return;
        int s = sources[nextSource];
        nextSource = (nextSource + 1) % sources.length;
        alSourceStop(s);
        alSourcei(s, AL_BUFFER, buffer);
        alSourcef(s, AL_GAIN, gain * volume);
        alSourcef(s, AL_PITCH, pitch);
        alSourcei(s, AL_SOURCE_RELATIVE, relative ? AL_TRUE : AL_FALSE);
        alSource3f(s, AL_POSITION, (float) x, (float) y, (float) z);
        alSourcef(s, AL_REFERENCE_DISTANCE, 4f);
        alSourcef(s, AL_MAX_DISTANCE, 32f);
        alSourcePlay(s);
    }

    public void listener(double x, double y, double z, float yaw, float pitch) {
        if (!ok) return;
        alListener3f(AL_POSITION, (float) x, (float) y, (float) z);
        double ry = Math.toRadians(yaw), rp = Math.toRadians(pitch);
        float fx = (float) (-Math.sin(ry) * Math.cos(rp)), fy = (float) -Math.sin(rp), fz = (float) (Math.cos(ry) * Math.cos(rp));
        alListenerfv(AL_ORIENTATION, new float[]{fx, fy, fz, 0, 1, 0});
    }

    public void dig(Block b, double x, double y, double z) {
        int[] set = dig.get(b.sound);
        if (set != null) play(set[random.nextInt(set.length)], 1f, 0.8f + random.nextFloat() * 0.2f, x, y, z, false);
    }

    public void hit(Block b, double x, double y, double z) {
        int[] set = step.get(b.sound);
        if (set != null) play(set[random.nextInt(set.length)], 0.3f, 0.5f, x, y, z, false);
    }

    public void step(Block b, double x, double y, double z) {
        int[] set = step.get(b.sound);
        if (set != null) play(set[random.nextInt(set.length)], 0.25f, 0.9f + random.nextFloat() * 0.2f, x, y, z, false);
    }

    public void splash() { play(splash, 0.5f, 0.9f + random.nextFloat() * 0.2f, 0, 0, 0, true); }

    public void click() { play(click, 0.5f, 1f, 0, 0, 0, true); }

    public void destroy() {
        if (!ok) return;
        alcMakeContextCurrent(0);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }
}
