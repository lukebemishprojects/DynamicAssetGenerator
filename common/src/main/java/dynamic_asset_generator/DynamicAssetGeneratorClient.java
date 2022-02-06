package dynamic_asset_generator;

import dynamic_asset_generator.client.api.DynAssetGeneratorClientAPI;
import dynamic_asset_generator.client.api.ForegroundTransferType;
import dynamic_asset_generator.client.api.PaletteExtractor;
import dynamic_asset_generator.client.util.IPalettePlan;
import net.minecraft.resources.ResourceLocation;

public class DynamicAssetGeneratorClient {
    public static void init() {
        //testing

        PaletteExtractor extractor = new PaletteExtractor(new ResourceLocation("minecraft","textures/block/stone.png"),
                new ResourceLocation("minecraft","textures/block/diamond_ore.png"), 6);
        IPalettePlan plan = new ForegroundTransferType(extractor, new ResourceLocation("minecraft","textures/block/granite.png"),
                true, false);
        DynAssetGeneratorClientAPI.planPaletteCombinedImage(new ResourceLocation("minecraft","textures/block/end_stone.png"), plan);

        /*
        DynAssetGenClientPlanner.planPaletteCombinedImage(new ResourceLocation("minecraft","textures/item/gold_ingot.png"),
                new PlannedPaletteCombinedImage(new ResourceLocation("minecraft","textures/block/moss_block.png"), new ResourceLocation("dynamic_asset_generator","textures/empty.png"), new ResourceLocation("minecraft","textures/item/copper_ingot.png"), false, 6, true));
        */
    }
}
