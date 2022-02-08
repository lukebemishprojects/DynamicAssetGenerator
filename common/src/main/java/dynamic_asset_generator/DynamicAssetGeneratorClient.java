package dynamic_asset_generator;

public class DynamicAssetGeneratorClient {
    public static void init() {
        //testing
/*
        String background = "textures/block/andesite.png";
        PaletteExtractor extractor = new PaletteExtractor(new ResourceLocation("minecraft","textures/block/stone.png"),
                new ResourceLocation("minecraft","textures/block/redstone_ore.png"),0,true, true);
        IPalettePlan plan = new ForegroundTransferType(extractor, new ResourceLocation("minecraft",background),
                true, false);
        PaletteExtractor extractor2 = new PaletteExtractor(new ResourceLocation("minecraft","textures/block/stone.png"),
                new ResourceLocation("minecraft","textures/block/redstone_ore.png"), 0,true, false);
        IPalettePlan plan2 = new ForegroundTransferType(extractor2, new ResourceLocation("minecraft",background),
                true, false);
        PaletteExtractor extractor3 = new PaletteExtractor(new ResourceLocation("minecraft","textures/block/stone.png"),
                new ResourceLocation("minecraft","textures/block/redstone_ore.png"), 0);
        IPalettePlan plan3 = new ForegroundTransferType(extractor3, new ResourceLocation("minecraft",background),
                true, false);
        DynAssetGeneratorClientAPI.planPaletteCombinedImage(new ResourceLocation("minecraft","textures/block/end_stone.png"), plan);
        DynAssetGeneratorClientAPI.planPaletteCombinedImage(new ResourceLocation("minecraft","textures/block/cobblestone.png"), plan2);
        DynAssetGeneratorClientAPI.planPaletteCombinedImage(new ResourceLocation("minecraft","textures/block/tuff.png"), plan3);
*/
        /*
        DynAssetGenClientPlanner.planPaletteCombinedImage(new ResourceLocation("minecraft","textures/item/gold_ingot.png"),
                new PlannedPaletteCombinedImage(new ResourceLocation("minecraft","textures/block/moss_block.png"), new ResourceLocation("dynamic_asset_generator","textures/empty.png"), new ResourceLocation("minecraft","textures/item/copper_ingot.png"), false, 6, true));
        */
    }
}
