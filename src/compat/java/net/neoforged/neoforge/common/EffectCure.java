package net.neoforged.neoforge.common;

import com.mojang.serialization.Codec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Something that removes effects, like milk (reamc-compat). */
public final class EffectCure {
    private static final Map<String, EffectCure> CURES = new ConcurrentHashMap<>();
    public static final Codec<EffectCure> CODEC = Codec.STRING.xmap(EffectCure::get, EffectCure::name);

    private final String name;

    private EffectCure(String name) { this.name = name; }

    public static EffectCure get(String name) { return CURES.computeIfAbsent(name, EffectCure::new); }

    public String name() { return name; }
}
