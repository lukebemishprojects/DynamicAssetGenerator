package dynamic_asset_generator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class DynamicAssetGeneratorClient {
    public static void init() {
        //testing
        /*
        DynAssetGenPlanner.planPaletteCombinedImage(new ResourceLocation("minecraft","textures/item/gold_ingot.png"),
                new PlannedPaletteCombinedImage(new ResourceLocation("minecraft","textures/item/amethyst_shard.png"), new ResourceLocation("dynamic_asset_generator","textures/empty.png"), new ResourceLocation("minecraft","textures/item/copper_ingot.png"), false, 6, true));
        */
    }
}
