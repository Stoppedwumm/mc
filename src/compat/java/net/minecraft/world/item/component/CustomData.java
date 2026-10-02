package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

/** Free-form NBT on a stack (reamc-compat). */
public final class CustomData {
    public static final CustomData EMPTY = new CustomData(new CompoundTag());
    public static final Codec<CustomData> CODEC = Codec.STRING.xmap(s -> new CustomData((CompoundTag) CompoundTag.reamc$fromJson(s)), d -> CompoundTag.reamc$toJson(d.tag));

    private final CompoundTag tag;

    private CustomData(CompoundTag tag) { this.tag = tag; }

    public static CustomData of(CompoundTag t) { return new CustomData(t.copy()); }

    public CompoundTag copyTag() { return tag.copy(); }

    public CompoundTag getUnsafe() { return tag; }

    public boolean isEmpty() { return tag.isEmpty(); }

    public boolean contains(String key) { return tag.contains(key); }

    public CustomData update(Consumer<CompoundTag> f) {
        CompoundTag c = tag.copy();
        f.accept(c);
        return new CustomData(c);
    }

    public static void update(net.minecraft.core.component.DataComponentType<CustomData> type, net.minecraft.world.item.ItemStack stack, Consumer<CompoundTag> f) {
        CustomData d = stack.getOrDefault(type, EMPTY).update(f);
        if (d.tag.isEmpty()) stack.remove(type);
        else stack.set(type, d);
    }

    @Override public boolean equals(Object o) { return o instanceof CustomData d && CompoundTag.reamc$toJson(d.tag).equals(CompoundTag.reamc$toJson(tag)); }
    @Override public int hashCode() { return CompoundTag.reamc$toJson(tag).hashCode(); }
}
