package com.github.lukebemish.dynamic_asset_generator;

import com.github.lukebemish.dynamic_asset_generator.client.api.DynAssetGeneratorClientAPI;
import com.github.lukebemish.dynamic_asset_generator.client.api.JsonReaderAPI;
import com.github.lukebemish.dynamic_asset_generator.client.api.PlannedPaletteCombinedImage;
import com.github.lukebemish.dynamic_asset_generator.client.json.*;
import com.github.lukebemish.dynamic_asset_generator.client.util.IPalettePlan;
import net.minecraft.resources.ResourceLocation;

public class DynamicAssetGeneratorClient {
    public static void init() {
        JsonReaderAPI.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID,"texture"),new TextureReader());
        JsonReaderAPI.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID,"combined_paletted_image"),new CombinedPaletteImage());
        JsonReaderAPI.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID,"overlay"),new Overlay());
        JsonReaderAPI.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID,"mask"),new Mask());
        JsonReaderAPI.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID,"crop"),new Crop());
        JsonReaderAPI.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID,"transform"),new Transform());
        JsonReaderAPI.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID,"foreground_transfer"),new ForegroundTransfer());
        JsonReaderAPI.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID,"color"),new ColorSource());
        //testing

        IPalettePlan p = new PlannedPaletteCombinedImage(new ResourceLocation("minecraft","textures/block/stone.png"),
                new ResourceLocation("minecraft","textures/item/apple.png"), new ResourceLocation("minecraft","textures/item/gold_ingot.png"),true,0,true);
        DynAssetGeneratorClientAPI.planPaletteCombinedImage(new ResourceLocation("minecraft","textures/block/end_stone.png"),p);

/*
        String background = "textures/block/calcite.png";
        PaletteExtractor extractor = new PaletteExtractor(new ResourceLocation("minecraft","textures/block/stone.png"),
                new ResourceLocation("minecraft","textures/block/gold_ore.png"),6,true, true,0.2);
        IPalettePlan plan = new ForegroundTransferType(extractor, new ResourceLocation("minecraft",background),
                true, false);
        PaletteExtractor extractor2 = new PaletteExtractor(new ResourceLocation("minecraft","textures/block/stone.png"),
                new ResourceLocation("minecraft","textures/block/redstone_ore.png"), 6,true, true,0.2);
        IPalettePlan plan2 = new ForegroundTransferType(extractor2, new ResourceLocation("minecraft",background),
                true, false);
        PaletteExtractor extractor3 = new PaletteExtractor(new ResourceLocation("minecraft","textures/block/stone.png"),
                new ResourceLocation("minecraft","textures/block/copper_ore.png"), 6,true,true,0.2);
        IPalettePlan plan3 = new ForegroundTransferType(extractor3, new ResourceLocation("minecraft",background),
                true, false);
        PaletteExtractor extractor4 = new PaletteExtractor(new ResourceLocation("minecraft","textures/block/stone.png"),
                new ResourceLocation("minecraft","textures/block/emerald_ore.png"), 6,true,true,0.2);
        IPalettePlan plan4 = new ForegroundTransferType(extractor4, new ResourceLocation("minecraft",background),
                true, false);
        DynAssetGeneratorClientAPI.planPaletteCombinedImage(new ResourceLocation("minecraft","textures/block/end_stone.png"), plan);
        DynAssetGeneratorClientAPI.planPaletteCombinedImage(new ResourceLocation("minecraft","textures/block/cobblestone.png"), plan2);
        DynAssetGeneratorClientAPI.planPaletteCombinedImage(new ResourceLocation("minecraft","textures/block/tuff.png"), plan3);
        DynAssetGeneratorClientAPI.planPaletteCombinedImage(new ResourceLocation("minecraft","textures/block/netherrack.png"), plan4);
*/
        /*
        DynAssetGenClientPlanner.planPaletteCombinedImage(new ResourceLocation("minecraft","textures/item/gold_ingot.png"),
                new PlannedPaletteCombinedImage(new ResourceLocation("minecraft","textures/block/moss_block.png"), new ResourceLocation("dynamic_asset_generator","textures/empty.png"), new ResourceLocation("minecraft","textures/item/copper_ingot.png"), false, 6, true));
        */
    }
}
