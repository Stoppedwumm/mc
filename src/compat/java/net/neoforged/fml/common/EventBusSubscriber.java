package net.neoforged.fml.common;

import net.neoforged.api.distmarker.Dist;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventBusSubscriber {
    Dist[] value() default {Dist.CLIENT, Dist.DEDICATED_SERVER};

    String modid() default "";

    Bus bus() default Bus.GAME;

    enum Bus { GAME, MOD }
}
