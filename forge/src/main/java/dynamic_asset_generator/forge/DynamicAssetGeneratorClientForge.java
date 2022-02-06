package dynamic_asset_generator.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import dynamic_asset_generator.DynamicAssetGenerator;
import dynamic_asset_generator.DynamicAssetGeneratorClient;

@Mod.EventBusSubscriber(modid = DynamicAssetGenerator.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DynamicAssetGeneratorClientForge {
    public static void init(final FMLClientSetupEvent event) {
        DynamicAssetGeneratorClient.init();
    }
}
