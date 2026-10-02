package net.minecraft.world.effect;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A status effect (reamc-compat). Effects reamc has natively run in the engine; others (mods' effects) are ticked
 * through {@link #applyEffectTick}.
 */
public class MobEffect implements net.minecraft.world.flag.FeatureElement {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Codec<Holder<MobEffect>> CODEC = (Codec) BuiltInRegistries.MOB_EFFECT.holderByNameCodec();
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<MobEffect>> STREAM_CODEC = (StreamCodec) ByteBufCodecs.holderRegistry((net.minecraft.resources.ResourceKey) net.minecraft.core.registries.Registries.MOB_EFFECT);

    private final MobEffectCategory category;
    private final int color;
    private final Map<Holder<Attribute>, AttributeTemplate> attributeModifiers = new LinkedHashMap<>();
    private String descriptionId;
    /** The engine effect this stands for (built-in effects reamc has). */
    public mc.entity.Effect reamc$engine;

    protected MobEffect(MobEffectCategory category, int color) {
        this.category = category;
        this.color = color;
    }

    protected MobEffect(MobEffectCategory category, int color, net.minecraft.core.particles.ParticleOptions particle) { this(category, color); }

    public int getBlendDurationTicks() { return 0; }

    /** Called while active (when {@link #shouldApplyEffectTickThisTick} says so); false removes the effect. */
    public boolean applyEffectTick(LivingEntity entity, int amplifier) { return true; }

    public void applyInstantenousEffect(Entity source, Entity indirect, LivingEntity target, int amplifier, double health) { applyEffectTick(target, amplifier); }

    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return false; }

    public void onEffectStarted(LivingEntity entity, int amplifier) { }
    public void onEffectAdded(LivingEntity entity, int amplifier) { }
    public void onMobRemoved(LivingEntity entity, int amplifier, Entity.RemovalReason reason) { }
    public void onMobHurt(LivingEntity entity, int amplifier, DamageSource source, float amount) { }

    public boolean isInstantenous() { return false; }

    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(this);
            descriptionId = id == null ? "effect.unregistered" : id.toLanguageKey("effect");
        }
        return descriptionId;
    }

    public String getDescriptionId() { return getOrCreateDescriptionId(); }
    public Component getDisplayName() { return Component.translatable(getDescriptionId()); }
    public MobEffectCategory getCategory() { return category; }
    public int getColor() { return color; }
    public boolean isBeneficial() { return category == MobEffectCategory.BENEFICIAL; }

    public MobEffect addAttributeModifier(Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        attributeModifiers.put(attribute, new AttributeTemplate(id, amount, op));
        return this;
    }

    public MobEffect setBlendDuration(int ticks) { return this; }

    public void createModifiers(int amplifier, BiConsumer<Holder<Attribute>, AttributeModifier> out) {
        attributeModifiers.forEach((a, t) -> out.accept(a, new AttributeModifier(t.id, t.amount * (amplifier + 1), t.operation)));
    }

    public MobEffect withSoundOnAdded(SoundEvent sound) { return this; }

    public net.minecraft.core.particles.ParticleOptions createParticleOptions(MobEffectInstance i) { return null; }

    public MobEffect requiredFeatures(net.minecraft.world.flag.FeatureFlag... flags) { return this; }

    @Override public net.minecraft.world.flag.FeatureFlagSet requiredFeatures() { return net.minecraft.world.flag.FeatureFlagSet.of(); }

    record AttributeTemplate(ResourceLocation id, double amount, AttributeModifier.Operation operation) { }
}
