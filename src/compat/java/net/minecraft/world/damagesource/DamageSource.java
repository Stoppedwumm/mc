package net.minecraft.world.damagesource;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** A cause of damage (reamc-compat: becomes the closest engine damage source). */
public class DamageSource {
    private final Holder<DamageType> type;
    private final Entity causing, direct;
    private final Vec3 position;

    public DamageSource(Holder<DamageType> type, Entity direct, Entity causing, Vec3 position) {
        this.type = type;
        this.direct = direct;
        this.causing = causing;
        this.position = position;
    }

    public DamageSource(Holder<DamageType> type, Entity direct, Entity causing) { this(type, direct, causing, null); }
    public DamageSource(Holder<DamageType> type, Vec3 position) { this(type, null, null, position); }
    public DamageSource(Holder<DamageType> type, Entity entity) { this(type, entity, entity); }
    public DamageSource(Holder<DamageType> type) { this(type, null, null, null); }

    public boolean isDirect() { return causing == direct; }
    public Entity getDirectEntity() { return direct; }
    public Entity getEntity() { return causing; }
    public ItemStack getWeaponItem() { return direct instanceof LivingEntity l ? l.getMainHandItem() : null; }
    public String getMsgId() { return type().msgId(); }
    public float getFoodExhaustion() { return type().exhaustion(); }
    public boolean scalesWithDifficulty() { return false; }
    public boolean isCreativePlayer() { return causing instanceof Player p && p.isCreative(); }
    public Vec3 getSourcePosition() { return position != null ? position : direct != null ? direct.position() : null; }
    public Vec3 sourcePositionRaw() { return position; }
    public boolean is(TagKey<DamageType> tag) { return type.is(tag); }
    public boolean is(ResourceKey<DamageType> key) { return type.is(key); }
    public DamageType type() { return type.value(); }
    public Holder<DamageType> typeHolder() { return type; }

    public Component getLocalizedDeathMessage(LivingEntity killed) {
        String key = "death.attack." + getMsgId();
        return causing != null ? Component.translatable(key + ".player", killed.getDisplayName(), causing.getDisplayName()) : Component.translatable(key, killed.getDisplayName());
    }

    /** The engine's damage source for this one. */
    public mc.entity.DamageSource reamc$engineSource() {
        String id = type.unwrapKey().map(k -> k.location().getPath()).orElse(getMsgId());
        return switch (id) {
            case "fall", "fly_into_wall", "stalagmite" -> mc.entity.DamageSource.FALL;
            case "drown", "dry_out" -> mc.entity.DamageSource.DROWN;
            case "lava" -> mc.entity.DamageSource.LAVA;
            case "in_fire", "on_fire", "campfire", "hot_floor", "lightning_bolt" -> mc.entity.DamageSource.FIRE;
            case "cactus", "sweet_berry_bush" -> mc.entity.DamageSource.CACTUS;
            case "starve" -> mc.entity.DamageSource.STARVE;
            case "explosion", "player_explosion", "fireworks", "bad_respawn_point" -> mc.entity.DamageSource.EXPLOSION;
            case "out_of_world", "outside_border", "generic_kill" -> mc.entity.DamageSource.VOID;
            case "magic", "indirect_magic", "wither", "dragon_breath", "thorns", "sonic_boom" -> mc.entity.DamageSource.MAGIC;
            case "arrow", "trident" -> mc.entity.DamageSource.ARROW;
            case "player_attack", "mob_attack", "mob_attack_no_aggro", "sting", "thrown", "mob_projectile", "spit" -> mc.entity.DamageSource.ATTACK;
            default -> mc.entity.DamageSource.GENERIC;
        };
    }

    @Override public String toString() { return "DamageSource (" + getMsgId() + ")"; }
}
