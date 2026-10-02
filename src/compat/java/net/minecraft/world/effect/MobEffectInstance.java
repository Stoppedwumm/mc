package net.minecraft.world.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

/** An active effect: which, how strong and for how long (reamc-compat). */
public class MobEffectInstance implements Comparable<MobEffectInstance> {
    public static final int INFINITE_DURATION = -1, MIN_AMPLIFIER = 0, MAX_AMPLIFIER = 255;
    public static final Codec<MobEffectInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
            MobEffect.CODEC.fieldOf("id").forGetter(MobEffectInstance::getEffect),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(MobEffectInstance::getAmplifier),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(MobEffectInstance::getDuration),
            Codec.BOOL.optionalFieldOf("ambient", false).forGetter(MobEffectInstance::isAmbient),
            Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(MobEffectInstance::isVisible),
            Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(MobEffectInstance::showIcon)
    ).apply(i, MobEffectInstance::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MobEffectInstance> STREAM_CODEC = StreamCodec.composite(
            MobEffect.STREAM_CODEC, MobEffectInstance::getEffect,
            ByteBufCodecs.VAR_INT, MobEffectInstance::getAmplifier,
            ByteBufCodecs.VAR_INT, MobEffectInstance::getDuration,
            ByteBufCodecs.BOOL, MobEffectInstance::isAmbient,
            ByteBufCodecs.BOOL, MobEffectInstance::isVisible,
            ByteBufCodecs.BOOL, MobEffectInstance::showIcon,
            MobEffectInstance::new);

    private final Holder<MobEffect> effect;
    private int duration, amplifier;
    private boolean ambient, visible, showIcon;
    private final Set<net.neoforged.neoforge.common.EffectCure> cures = new HashSet<>();

    public MobEffectInstance(Holder<MobEffect> effect) { this(effect, 0, 0); }
    public MobEffectInstance(Holder<MobEffect> effect, int duration) { this(effect, duration, 0); }
    public MobEffectInstance(Holder<MobEffect> effect, int duration, int amplifier) { this(effect, duration, amplifier, false, true); }
    public MobEffectInstance(Holder<MobEffect> effect, int duration, int amplifier, boolean ambient, boolean visible) { this(effect, duration, amplifier, ambient, visible, visible); }
    public MobEffectInstance(Holder<MobEffect> effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) { this(effect, duration, amplifier, ambient, visible, showIcon, null); }

    public MobEffectInstance(Holder<MobEffect> effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon, MobEffectInstance hidden) {
        this.effect = effect;
        this.duration = duration;
        this.amplifier = Math.max(0, Math.min(255, amplifier));
        this.ambient = ambient;
        this.visible = visible;
        this.showIcon = showIcon;
        cures.add(net.neoforged.neoforge.common.EffectCures.MILK);
    }

    public MobEffectInstance(MobEffectInstance o) { this(o.effect, o.duration, o.amplifier, o.ambient, o.visible, o.showIcon); }

    public Set<net.neoforged.neoforge.common.EffectCure> getCures() { return cures; }
    public float getBlendFactor(LivingEntity e, float partial) { return 1; }
    public net.minecraft.core.particles.ParticleOptions getParticleOptions() { return null; }

    /** Takes over a stronger or longer effect of the same kind; true when something changed. */
    public boolean update(MobEffectInstance o) {
        boolean changed = false;
        if (o.amplifier > amplifier) { amplifier = o.amplifier; duration = o.duration; changed = true; }
        else if (o.amplifier == amplifier && (o.duration > duration || o.isInfiniteDuration())) { duration = o.duration; changed = true; }
        return changed;
    }

    public boolean isInfiniteDuration() { return duration == INFINITE_DURATION; }
    public boolean endsWithin(int ticks) { return !isInfiniteDuration() && duration <= ticks; }
    public int mapDuration(it.unimi.dsi.fastutil.ints.Int2IntFunction f) { return isInfiniteDuration() ? duration : f.applyAsInt(duration); }
    public Holder<MobEffect> getEffect() { return effect; }
    public int getDuration() { return duration; }
    public int getAmplifier() { return amplifier; }
    public boolean isAmbient() { return ambient; }
    public boolean isVisible() { return visible; }
    public boolean showIcon() { return showIcon; }

    /** One tick: applies the effect when due and counts down; false once it has run out. */
    public boolean tick(LivingEntity entity, Runnable onUpdate) {
        if (duration == 0) return false;
        if (effect.value().shouldApplyEffectTickThisTick(duration, amplifier) && !effect.value().applyEffectTick(entity, amplifier)) return false;
        if (!isInfiniteDuration()) duration--;
        return duration != 0;
    }

    public void onEffectStarted(LivingEntity e) { effect.value().onEffectStarted(e, amplifier); }
    public void onEffectAdded(LivingEntity e) { effect.value().onEffectAdded(e, amplifier); }
    public void onMobRemoved(LivingEntity e, Entity.RemovalReason reason) { effect.value().onMobRemoved(e, amplifier, reason); }
    public void onMobHurt(LivingEntity e, DamageSource source, float amount) { effect.value().onMobHurt(e, amplifier, source, amount); }
    public String getDescriptionId() { return effect.value().getDescriptionId(); }
    public boolean is(Holder<MobEffect> h) { return effect.equals(h) || effect.value() == h.value(); }
    public void copyBlendState(MobEffectInstance o) { }
    public void skipBlending() { }

    public Tag save() {
        CompoundTag t = new CompoundTag();
        t.putString("id", effect.getRegisteredName());
        t.putInt("amplifier", amplifier);
        t.putInt("duration", duration);
        t.putBoolean("ambient", ambient);
        t.putBoolean("show_particles", visible);
        t.putBoolean("show_icon", showIcon);
        return t;
    }

    public static MobEffectInstance load(CompoundTag t) {
        var h = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(t.getString("id")));
        if (h.isEmpty()) return null;
        @SuppressWarnings({"unchecked", "rawtypes"})
        Holder<MobEffect> e = (Holder) h.get();
        return new MobEffectInstance(e, t.getInt("duration"), t.getInt("amplifier"), t.getBoolean("ambient"), t.getBoolean("show_particles"), t.getBoolean("show_icon"));
    }

    @Override public int compareTo(MobEffectInstance o) { return Integer.compare(o.duration, duration); }
    @Override public boolean equals(Object o) { return o instanceof MobEffectInstance m && m.effect.equals(effect) && m.duration == duration && m.amplifier == amplifier; }
    @Override public int hashCode() { return effect.hashCode() * 31 + duration * 7 + amplifier; }
    @Override public String toString() { return getDescriptionId() + (amplifier > 0 ? " x " + (amplifier + 1) : "") + ", Duration: " + duration; }
}
