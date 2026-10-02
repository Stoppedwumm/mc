package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** A world as mods see it; reamc-compat backs it with an engine world (see mc.mod.Bridge). */
public abstract class Level implements LevelAccessor, AutoCloseable {
    public static final int MAX_LEVEL_SIZE = 30000000;
    public static final ResourceKey<Level> OVERWORLD = key("overworld"), NETHER = key("the_nether"), END = key("the_end");

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ResourceKey<Level> key(String id) {
        return ResourceKey.create((ResourceKey) net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.ResourceLocation.withDefaultNamespace(id));
    }

    public final boolean isClientSide;
    public final RandomSource random = RandomSource.create();
    private DamageSources damageSources;

    protected Level(boolean isClientSide) { this.isClientSide = isClientSide; }

    /** The engine world behind this level. */
    public abstract mc.world.World reamc$world();

    @Override public boolean isClientSide() { return isClientSide; }

    public ResourceKey<Level> dimension() { return reamc$world().dimension == mc.world.Dimension.NETHER ? NETHER : OVERWORLD; }

    // ------------------------------------------------------------------ blocks

    @Override
    public BlockState getBlockState(BlockPos pos) {
        mc.world.World w = reamc$world();
        return mc.mod.Bridge.state(w.getBlock(pos.getX(), pos.getY(), pos.getZ()), w.getMeta(pos.getX(), pos.getY(), pos.getZ()));
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        mc.world.World w = reamc$world();
        int id = w.getBlock(pos.getX(), pos.getY(), pos.getZ()), meta = w.getMeta(pos.getX(), pos.getY(), pos.getZ());
        FluidState f = Fluids.reamc$of(id, meta);
        if (!f.isEmpty()) return f;
        return mc.world.Block.get(id).modded ? getBlockState(pos).getFluidState() : f;
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft) {
        mc.world.Block b = mc.mod.Bridge.engineBlock(state.getBlock());
        if (b == null) return false;
        return reamc$world().setBlock(pos.getX(), pos.getY(), pos.getZ(), b.id, state.reamc$index(), (flags & UPDATE_NEIGHBORS) != 0);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags) { return setBlock(pos, state, flags, 512); }

    public boolean setBlockAndUpdate(BlockPos pos, BlockState state) { return setBlock(pos, state, UPDATE_ALL); }

    @Override
    public boolean removeBlock(BlockPos pos, boolean moved) {
        BlockState fluid = getFluidState(pos).createLegacyBlock();
        return setBlock(pos, fluid, UPDATE_ALL);
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean drop, Entity breaker, int recursionLeft) {
        if (getBlockState(pos).isAir()) return false;
        reamc$world().breakBlock(pos.getX(), pos.getY(), pos.getZ(), null, drop);
        return true;
    }

    @Override public boolean isEmptyBlock(BlockPos pos) { return getBlockState(pos).isAir(); }

    @Override public BlockEntity getBlockEntity(BlockPos pos) { return mc.mod.Bridge.blockEntity(reamc$world(), pos); }

    public void removeBlockEntity(BlockPos pos) { mc.mod.Bridge.removeBlockEntity(reamc$world(), pos); }

    public void setBlockEntity(BlockEntity be) { mc.mod.Bridge.putBlockEntity(reamc$world(), be); }

    public void updateNeighbourForOutputSignal(BlockPos pos, Block block) { reamc$world().notifyAround(pos.getX(), pos.getY(), pos.getZ()); }

    public void updateNeighborsAt(BlockPos pos, Block block) { reamc$world().notifyAround(pos.getX(), pos.getY(), pos.getZ()); }

    public void updateNeighborsAtExceptFromFacing(BlockPos pos, Block block, Direction except) { updateNeighborsAt(pos, block); }

    public void neighborChanged(BlockPos pos, Block block, BlockPos from) { mc.mod.Bridge.neighborChanged(this, pos, block, from); }

    @Override public void blockUpdated(BlockPos pos, Block block) { updateNeighborsAt(pos, block); }

    public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) { mc.mod.Bridge.blockEntityChanged(reamc$world(), pos); }

    public void setBlocksDirty(BlockPos pos, BlockState oldState, BlockState newState) { }

    public void blockEvent(BlockPos pos, Block block, int id, int param) { getBlockState(pos).triggerEvent(this, pos, id, param); }

    @Override public void scheduleTick(BlockPos pos, Block block, int delay) { reamc$world().scheduleTick(pos.getX(), pos.getY(), pos.getZ(), delay); }

    @Override
    public int getSignal(BlockPos pos, Direction from) {
        return mc.world.Redstone.powered(reamc$world(), pos.getX(), pos.getY(), pos.getZ()) ? 15 : 0;
    }

    @Override
    public boolean hasNeighborSignal(BlockPos pos) { return mc.world.Redstone.powered(reamc$world(), pos.getX(), pos.getY(), pos.getZ()); }

    @Override public int getBestNeighborSignal(BlockPos pos) { return hasNeighborSignal(pos) ? 15 : 0; }

    // ------------------------------------------------------------------ light, sky, chunks

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        mc.world.World w = reamc$world();
        return layer == LightLayer.SKY ? w.getSkyLight(pos.getX(), pos.getY(), pos.getZ()) : w.getBlockLight(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public int getSkyDarken() {
        float f = mc.world.World.dayFactor(reamc$world().time, reamc$world().raining ? 1 : 0);
        return (int) ((1 - f) * 11);
    }

    @Override
    public int getHeight(Heightmap.Types type, int x, int z) {
        mc.world.Chunk c = reamc$world().getChunk(x >> 4, z >> 4);
        if (c == null) return getMinBuildHeight();
        for (int y = mc.world.Chunk.HEIGHT - 1; y >= 0; y--) {
            mc.world.Block b = mc.world.Block.get(c.get(x & 15, y, z & 15));
            if (b == mc.world.Block.AIR) continue;
            boolean counts = switch (type) {
                case WORLD_SURFACE, WORLD_SURFACE_WG -> true;
                case OCEAN_FLOOR, OCEAN_FLOOR_WG -> b.solid;
                case MOTION_BLOCKING -> b.solid || b.model == mc.world.Block.Model.LIQUID;
                case MOTION_BLOCKING_NO_LEAVES -> (b.solid || b.model == mc.world.Block.Model.LIQUID) && b.layer != mc.world.Block.Layer.CUTOUT;
            };
            if (counts) return y + 1;
        }
        return 0;
    }

    @Override public Holder<Biome> getBiome(BlockPos pos) { return mc.mod.Bridge.biome(reamc$world().biomeAt(pos.getX(), pos.getZ())); }

    @Override public boolean hasChunk(int cx, int cz) { return reamc$world().isLoaded(cx << 4, cz << 4); }

    public net.minecraft.world.level.chunk.LevelChunk getChunkAt(BlockPos pos) { return getChunk(pos.getX() >> 4, pos.getZ() >> 4); }

    public net.minecraft.world.level.chunk.LevelChunk getChunk(int cx, int cz) { return new net.minecraft.world.level.chunk.LevelChunk(this, new ChunkPos(cx, cz)); }

    @Override public int getHeight() { return mc.world.Chunk.HEIGHT; }
    @Override public int getMinBuildHeight() { return 0; }

    @Override public RegistryAccess registryAccess() { return mc.mod.Bridge.registryAccess(); }

    public DamageSources damageSources() {
        if (damageSources == null) damageSources = new DamageSources(registryAccess());
        return damageSources;
    }

    public net.minecraft.world.item.crafting.RecipeManager getRecipeManager() { return mc.mod.Bridge.recipeManager(); }

    public GameRules getGameRules() { return GameRules.DEFAULTS; }

    // ------------------------------------------------------------------ time and weather

    public long getGameTime() { return reamc$world().tickCount; }
    public long getDayTime() { return reamc$world().time; }
    @Override public long dayTime() { return reamc$world().time; }
    public boolean isDay() { return reamc$world().isDaytime(); }
    public boolean isNight() { return !isDay(); }
    public boolean isRaining() { return reamc$world().raining; }
    public boolean isThundering() { return false; }
    public float getRainLevel(float partial) { return isRaining() ? 1 : 0; }

    public boolean isRainingAt(BlockPos pos) {
        return isRaining() && canSeeSky(pos) && getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY()
                && getBiome(pos).value().hasPrecipitation() && getBiome(pos).value().getBaseTemperature() >= 0.15f;
    }

    @Override public RandomSource getRandom() { return random; }

    @Override public MinecraftServer getServer() { return mc.mod.Bridge.server(); }

    public BlockPos getSharedSpawnPos() { return new BlockPos(0, getHeight(Heightmap.Types.WORLD_SURFACE, 0, 0), 0); }

    // ------------------------------------------------------------------ entities

    public Entity getEntity(int id) { return mc.mod.Bridge.entity(reamc$world(), id); }

    @Override public List<? extends Player> players() { return mc.mod.Bridge.players(reamc$world()); }

    @Override
    public List<Entity> getEntities(Entity except, AABB box, Predicate<? super Entity> filter) {
        List<Entity> out = new ArrayList<>();
        for (mc.entity.Entity e : new ArrayList<>(reamc$world().entities())) {
            Entity w = mc.mod.Bridge.wrap(e);
            if (w == null || w == except || e.removed || !w.getBoundingBox().intersects(box)) continue;
            if (filter.test(w)) out.add(w);
        }
        for (mc.entity.Player p : reamc$world().players()) {
            Entity w = mc.mod.Bridge.wrap(p);
            if (w != except && w.getBoundingBox().intersects(box) && filter.test(w) && !out.contains(w)) out.add(w);
        }
        return out;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> type, AABB box, Predicate<? super T> filter) {
        List<T> out = new ArrayList<>();
        for (Entity e : getEntities(null, box, x -> true)) if (type.isInstance(e) && filter.test((T) e)) out.add((T) e);
        return out;
    }

    @Override public boolean addFreshEntity(Entity e) { return mc.mod.Bridge.addEntity(reamc$world(), e); }

    public void broadcastEntityEvent(Entity e, byte event) { }

    // ------------------------------------------------------------------ sounds, particles, events

    @Override
    public void playSound(Player except, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
        playSound(except, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, source, volume, pitch);
    }

    public void playSound(Player except, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch) {
        String n = sound.reamc$name();
        if (!n.isEmpty()) reamc$world().playSound(n, x, y, z, volume, pitch);
    }

    public void playSound(Player except, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) {
        playSound(except, x, y, z, sound.value(), source, volume, pitch);
    }

    public void playSound(Player except, Entity at, SoundEvent sound, SoundSource source, float volume, float pitch) {
        playSound(except, at.getX(), at.getY(), at.getZ(), sound, source, volume, pitch);
    }

    public void playSound(Entity except, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
        playSound((Player) null, pos, sound, source, volume, pitch);
    }

    public void playLocalSound(double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay) {
        playSound(null, x, y, z, sound, source, volume, pitch);
    }

    public void playLocalSound(BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay) {
        playSound(null, pos, sound, source, volume, pitch);
    }

    public void playSeededSound(Player except, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {
        playSound(except, x, y, z, sound.value(), source, volume, pitch);
    }

    @Override
    public void addParticle(ParticleOptions particle, double x, double y, double z, double vx, double vy, double vz) {
        reamc$world().addParticle(particle.reamc$name(), x, y, z);
    }

    public void addParticle(ParticleOptions particle, boolean force, double x, double y, double z, double vx, double vy, double vz) { addParticle(particle, x, y, z, vx, vy, vz); }

    public void addAlwaysVisibleParticle(ParticleOptions particle, double x, double y, double z, double vx, double vy, double vz) { addParticle(particle, x, y, z, vx, vy, vz); }

    /** Minecraft's numbered world events: the common ones become reamc sounds and particles. */
    @Override
    public void levelEvent(Player except, int type, BlockPos pos, int data) {
        mc.world.World w = reamc$world();
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        switch (type) {
            case 2001 -> {
                BlockState s = net.minecraft.world.level.block.Block.stateById(data);
                mc.world.Block b = s.getBlock().reamc$block;
                if (b != null && w.listener != null) w.listener.blockBroken(pos.getX(), pos.getY(), pos.getZ(), b, s.reamc$index());
            }
            case 1501, 1502 -> w.playSound("fizz", x, y, z, 0.5f, 2.6f);
            case 2005, 1505 -> { for (int i = 0; i < 6; i++) w.addParticle("happy", pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble()); }
            case 1009 -> w.playSound("fizz", x, y, z, 0.5f, 2.6f);
            default -> { }
        }
    }

    @Override public void gameEvent(Holder<GameEvent> event, Vec3 pos, GameEvent.Context context) { }

    @Override public void close() { }

    public boolean mayInteract(Player player, BlockPos pos) { return true; }

    public void explode(Entity source, double x, double y, double z, float power, ExplosionInteraction interaction) {
        reamc$world().explode(x, y, z, power, source == null ? null : source.reamc$engine());
    }

    public enum ExplosionInteraction { NONE, BLOCK, MOB, TNT, TRIGGER }

    public static final int UPDATE_NEIGHBORS = 1, UPDATE_CLIENTS = 2, UPDATE_INVISIBLE = 4, UPDATE_IMMEDIATE = 8, UPDATE_KNOWN_SHAPE = 16,
            UPDATE_SUPPRESS_DROPS = 32, UPDATE_MOVE_BY_PISTON = 64, UPDATE_NONE = 4, UPDATE_ALL = 3, UPDATE_ALL_IMMEDIATE = 11;

    // NeoForge's ILevelExtension
    public <T, C> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, C> cap, BlockPos pos, C context) { return cap.getCapability(this, pos, null, null, context); }
    public <T, C> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, C> cap, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity be, C context) { return cap.getCapability(this, pos, state, be, context); }
    public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, Void> cap, BlockPos pos) { return cap.getCapability(this, pos, null, null, null); }
    public void invalidateCapabilities(BlockPos pos) { }
    public void invalidateCapabilities(net.minecraft.world.level.ChunkPos pos) { }
}
