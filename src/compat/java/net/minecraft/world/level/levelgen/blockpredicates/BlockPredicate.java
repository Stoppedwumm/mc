package net.minecraft.world.level.levelgen.blockpredicates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/** A test of the blocks around a position (reamc-compat: Minecraft's predicate types, read from JSON). */
public interface BlockPredicate extends BiPredicate<WorldGenLevel, BlockPos> {
    Codec<BlockPredicate> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<BlockPredicate, T>> decode(DynamicOps<T> ops, T input) {
            try {
                return DataResult.success(Pair.of(fromJson(new Dynamic<>(ops, input).convert(JsonOps.INSTANCE).getValue()), ops.empty()));
            } catch (RuntimeException e) {
                return DataResult.error(() -> "Bad block predicate: " + e.getMessage());
            }
        }

        @Override
        public <T> DataResult<T> encode(BlockPredicate p, DynamicOps<T> ops, T prefix) { return DataResult.error(() -> "Block predicates can't be written"); }
    };

    static BlockPredicate alwaysTrue() { return (l, p) -> true; }

    static BlockPredicate not(BlockPredicate p) { return (l, pos) -> !p.test(l, pos); }

    static BlockPredicate allOf(BlockPredicate... ps) { return (l, pos) -> { for (BlockPredicate p : ps) if (!p.test(l, pos)) return false; return true; }; }

    static BlockPredicate anyOf(BlockPredicate... ps) { return (l, pos) -> { for (BlockPredicate p : ps) if (p.test(l, pos)) return true; return false; }; }

    static BlockPredicate replaceable() { return (l, pos) -> l.getBlockState(pos).canBeReplaced(); }

    static BlockPredicate ONLY_IN_AIR_PREDICATE() { return (l, pos) -> l.getBlockState(pos).isAir(); }

    private static Vec3i offset(JsonObject o) {
        if (!o.has("offset")) return Vec3i.ZERO;
        JsonArray a = o.getAsJsonArray("offset");
        return new Vec3i(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    private static List<String> strings(JsonElement e) {
        List<String> l = new ArrayList<>();
        if (e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) l.add(x.getAsString());
        else l.add(e.getAsString());
        return l;
    }

    static BlockPredicate fromJson(JsonElement json) {
        JsonObject o = json.getAsJsonObject();
        String type = o.get("type").getAsString().replace("minecraft:", "");
        Vec3i off = offset(o);
        return switch (type) {
            case "true" -> alwaysTrue();
            case "not" -> not(fromJson(o.get("predicate")));
            case "all_of", "any_of" -> {
                List<BlockPredicate> l = new ArrayList<>();
                for (JsonElement e : o.getAsJsonArray("predicates")) l.add(fromJson(e));
                BlockPredicate[] a = l.toArray(BlockPredicate[]::new);
                yield type.equals("all_of") ? allOf(a) : anyOf(a);
            }
            case "matching_blocks" -> {
                List<String> ids = strings(o.get("blocks"));
                yield (l, pos) -> {
                    BlockState s = l.getBlockState(pos.offset(off));
                    for (String id : ids) {
                        if (id.startsWith("#") ? s.is(BlockTags.create(ResourceLocation.parse(id.substring(1)))) : s.getBlock().builtInRegistryHolder().is(ResourceLocation.parse(id))) return true;
                    }
                    return false;
                };
            }
            case "matching_block_tag" -> {
                var tag = BlockTags.create(ResourceLocation.parse(o.get("tag").getAsString()));
                yield (l, pos) -> l.getBlockState(pos.offset(off)).is(tag);
            }
            case "matching_fluids" -> {
                List<String> ids = strings(o.get("fluids"));
                yield (l, pos) -> {
                    var f = l.getFluidState(pos.offset(off)).getType();
                    var id = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(f);
                    return id != null && ids.contains(id.toString());
                };
            }
            case "replaceable" -> (l, pos) -> l.getBlockState(pos.offset(off)).canBeReplaced();
            case "solid" -> (l, pos) -> l.getBlockState(pos.offset(off)).isSolid();
            case "would_survive" -> {
                BlockState s = BlockState.CODEC.parse(JsonOps.INSTANCE, o.get("state")).getOrThrow();
                yield (l, pos) -> s.canSurvive(l, pos.offset(off));
            }
            case "has_sturdy_face" -> {
                Direction d = Direction.byName(o.get("direction").getAsString());
                yield (l, pos) -> l.getBlockState(pos.offset(off)).isFaceSturdy(l, pos.offset(off), d);
            }
            case "inside_world_bounds" -> (l, pos) -> !l.isOutsideBuildHeight(pos.offset(off));
            default -> {
                System.err.println("[mods] Unknown block predicate " + type + "; treating it as true");
                yield alwaysTrue();
            }
        };
    }
}
