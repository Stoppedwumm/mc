package fixture;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(modid = "fixture")
public class Setup {
    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) { FixtureMod.setupRan = true; }
}
