package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** A flower (reamc-compat). */
public class FlowerBlock extends BushBlock implements SuspiciousEffectHolder {
    public static final MapCodec<FlowerBlock> CODEC = simpleCodec(p -> new FlowerBlock(SuspiciousStewEffects.EMPTY, p));
    protected static final VoxelShape SHAPE = box(5, 0, 5, 11, 10, 11);
    private final SuspiciousStewEffects suspiciousStewEffects;

    public FlowerBlock(Holder<MobEffect> effect, float seconds, Properties properties) {
        this(makeEffectList(effect, seconds), properties);
    }

    public FlowerBlock(SuspiciousStewEffects effects, Properties properties) {
        super(properties);
        this.suspiciousStewEffects = effects;
    }

    @Override public MapCodec<? extends FlowerBlock> codec() { return CODEC; }

    protected static SuspiciousStewEffects makeEffectList(Holder<MobEffect> effect, float seconds) {
        return new SuspiciousStewEffects(List.of(new SuspiciousStewEffects.Entry(effect, net.minecraft.util.Mth.floor(seconds * 20))));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 o = state.getOffset(level, pos);
        return SHAPE.move(o.x, o.y, o.z);
    }

    @Override public SuspiciousStewEffects getSuspiciousEffects() { return suspiciousStewEffects; }
}
