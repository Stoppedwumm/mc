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
