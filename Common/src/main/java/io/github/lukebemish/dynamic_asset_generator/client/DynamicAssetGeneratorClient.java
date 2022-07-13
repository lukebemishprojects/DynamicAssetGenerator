package io.github.lukebemish.dynamic_asset_generator.client;

import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.api.client.AssetResourceCache;
import io.github.lukebemish.dynamic_asset_generator.api.client.GeneratedTextureHolder;
import io.github.lukebemish.dynamic_asset_generator.api.client.texsources.*;
import io.github.lukebemish.dynamic_asset_generator.platform.Services;
import net.minecraft.resources.ResourceLocation;

public class DynamicAssetGeneratorClient {
    private DynamicAssetGeneratorClient() {}

    public static void init() {
        GeneratedTextureHolder.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "texture"), TextureReader.CODEC);
        GeneratedTextureHolder.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "fallback"), FallbackSource.CODEC);
        GeneratedTextureHolder.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "combined_paletted_image"), CombinedPaletteImage.CODEC);
        GeneratedTextureHolder.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "overlay"), Overlay.CODEC);
        GeneratedTextureHolder.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask"), Mask.CODEC);
        GeneratedTextureHolder.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "crop"), Crop.CODEC);
        GeneratedTextureHolder.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "transform"), Transform.CODEC);
        GeneratedTextureHolder.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "foreground_transfer"), ForegroundTransfer.CODEC);
        GeneratedTextureHolder.registerTexSourceReadingType(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "color"), ColorSource.CODEC);

        testing();
    }

    private static void testing() {
        //testing

        if (Services.PLATFORM.isDev()) {
            AssetResourceCache.INSTANCE.planSource(GeneratedTextureHolder.of(new ResourceLocation("block/end_stone"),
                    new ForegroundTransfer(new TextureReader(new ResourceLocation("block/stone")),
                            new TextureReader(new ResourceLocation("block/redstone_ore")),
                            new TextureReader(new ResourceLocation("block/end_stone")),
                            6,true,true,true, 0.2d)));
            AssetResourceCache.INSTANCE.planSource(GeneratedTextureHolder.of(new ResourceLocation("block/tuff"),
                    new ForegroundTransfer(new TextureReader(new ResourceLocation("block/stone")),
                            new TextureReader(new ResourceLocation("block/coal_ore")),
                            new TextureReader(new ResourceLocation("block/end_stone")),
                            6,true,true,true, 0.2d)));
            AssetResourceCache.INSTANCE.planSource(GeneratedTextureHolder.of(new ResourceLocation("block/calcite"),
                    new ForegroundTransfer(new TextureReader(new ResourceLocation("block/stone")),
                            new TextureReader(new ResourceLocation("block/iron_ore")),
                            new TextureReader(new ResourceLocation("block/end_stone")),
                            6,true,true,true, 0.2d)));
        }
    }
}
