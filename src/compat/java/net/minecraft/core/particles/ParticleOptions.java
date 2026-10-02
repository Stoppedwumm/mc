package net.minecraft.core.particles;

/** A particle and its parameters (reamc-compat). */
public interface ParticleOptions {
    ParticleType<?> getType();

    /** reamc's particle name for this one. */
    default String reamc$name() {
        var id = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getKey(getType());
        String p = id == null ? "" : id.getPath();
        if (p.contains("smoke") || p.contains("ash")) return "smoke";
        if (p.contains("flame") || p.contains("lava") && !p.contains("drip")) return "flame";
        if (p.contains("bubble") || p.equals("underwater")) return "bubble";
        if (p.contains("splash") || p.contains("rain") || p.contains("fishing")) return "splash";
        if (p.contains("drip") || p.contains("falling")) return "drip";
        if (p.equals("heart")) return "heart";
        if (p.contains("villager") || p.equals("composter")) return "happy";
        if (p.contains("enchant")) return "enchant";
        if (p.contains("crit")) return "crit";
        if (p.contains("portal")) return "portal";
        if (p.contains("explosion")) return "explosion";
        if (p.contains("effect") || p.contains("witch")) return "magic";
        if (p.contains("slime")) return "slime";
        return "poof";
    }
}
