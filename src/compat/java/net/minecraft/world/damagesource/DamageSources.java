package net.minecraft.world.damagesource;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Factory for the usual damage sources (reamc-compat). */
public class DamageSources {
    private final Registry<DamageType> types;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DamageSources(RegistryAccess access) { types = access.registryOrThrow((ResourceKey) Registries.DAMAGE_TYPE); }

    public DamageSource source(ResourceKey<DamageType> key) { return new DamageSource(holder(key)); }
    public DamageSource source(ResourceKey<DamageType> key, Entity e) { return new DamageSource(holder(key), e); }
    public DamageSource source(ResourceKey<DamageType> key, Entity direct, Entity causing) { return new DamageSource(holder(key), direct, causing); }

    private Holder<DamageType> holder(ResourceKey<DamageType> key) {
        return types.getHolder(key).<Holder<DamageType>>map(h -> h).orElseGet(() -> Holder.direct(new DamageType(key.location().getPath(), 0.1f)));
    }

    public DamageSource inFire() { return source(DamageTypes.IN_FIRE); }
    public DamageSource campfire() { return source(DamageTypes.CAMPFIRE); }
    public DamageSource lightningBolt() { return source(DamageTypes.LIGHTNING_BOLT); }
    public DamageSource onFire() { return source(DamageTypes.ON_FIRE); }
    public DamageSource lava() { return source(DamageTypes.LAVA); }
    public DamageSource hotFloor() { return source(DamageTypes.HOT_FLOOR); }
    public DamageSource inWall() { return source(DamageTypes.IN_WALL); }
    public DamageSource cramming() { return source(DamageTypes.CRAMMING); }
    public DamageSource drown() { return source(DamageTypes.DROWN); }
    public DamageSource starve() { return source(DamageTypes.STARVE); }
    public DamageSource cactus() { return source(DamageTypes.CACTUS); }
    public DamageSource fall() { return source(DamageTypes.FALL); }
    public DamageSource flyIntoWall() { return source(DamageTypes.FLY_INTO_WALL); }
    public DamageSource fellOutOfWorld() { return source(DamageTypes.FELL_OUT_OF_WORLD); }
    public DamageSource generic() { return source(DamageTypes.GENERIC); }
    public DamageSource magic() { return source(DamageTypes.MAGIC); }
    public DamageSource wither() { return source(DamageTypes.WITHER); }
    public DamageSource dragonBreath() { return source(DamageTypes.DRAGON_BREATH); }
    public DamageSource dryOut() { return source(DamageTypes.DRY_OUT); }
    public DamageSource sweetBerryBush() { return source(DamageTypes.SWEET_BERRY_BUSH); }
    public DamageSource freeze() { return source(DamageTypes.FREEZE); }
    public DamageSource stalagmite() { return source(DamageTypes.STALAGMITE); }
    public DamageSource fallingBlock(Entity e) { return source(DamageTypes.FALLING_BLOCK, e); }
    public DamageSource anvil(Entity e) { return source(DamageTypes.FALLING_ANVIL, e); }
    public DamageSource fallingStalactite(Entity e) { return source(DamageTypes.FALLING_STALACTITE, e); }
    public DamageSource sting(LivingEntity e) { return source(DamageTypes.STING, e); }
    public DamageSource mobAttack(LivingEntity e) { return source(DamageTypes.MOB_ATTACK, e); }
    public DamageSource noAggroMobAttack(LivingEntity e) { return source(DamageTypes.MOB_ATTACK_NO_AGGRO, e); }
    public DamageSource playerAttack(Player p) { return source(DamageTypes.PLAYER_ATTACK, p); }
    public DamageSource trident(Entity direct, Entity causing) { return source(DamageTypes.TRIDENT, direct, causing); }
    public DamageSource mobProjectile(Entity direct, LivingEntity causing) { return source(DamageTypes.MOB_PROJECTILE, direct, causing); }
    public DamageSource spit(Entity direct, LivingEntity causing) { return source(DamageTypes.SPIT, direct, causing); }
    public DamageSource windCharge(Entity direct, LivingEntity causing) { return source(DamageTypes.WIND_CHARGE, direct, causing); }
    public DamageSource thrown(Entity direct, Entity causing) { return source(DamageTypes.THROWN, direct, causing); }
    public DamageSource indirectMagic(Entity direct, Entity causing) { return source(DamageTypes.INDIRECT_MAGIC, direct, causing); }
    public DamageSource thorns(Entity e) { return source(DamageTypes.THORNS, e); }
    public DamageSource explosion(Entity direct, Entity causing) { return source(causing != null ? DamageTypes.PLAYER_EXPLOSION : DamageTypes.EXPLOSION, direct, causing); }
    public DamageSource sonicBoom(Entity e) { return source(DamageTypes.SONIC_BOOM, e); }
    public DamageSource badRespawnPointExplosion(Vec3 pos) { return new DamageSource(holder(DamageTypes.BAD_RESPAWN_POINT), pos); }
    public DamageSource outOfBorder() { return source(DamageTypes.OUTSIDE_BORDER); }
    public DamageSource genericKill() { return source(DamageTypes.GENERIC_KILL); }
}
