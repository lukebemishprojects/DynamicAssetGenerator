package io.github.lukebemish.dynamic_asset_generator.impl.client;

import io.github.lukebemish.dynamic_asset_generator.api.client.AssetResourceCache;
import io.github.lukebemish.dynamic_asset_generator.api.client.DynamicTextureSource;
import io.github.lukebemish.dynamic_asset_generator.api.client.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.api.client.texsources.*;
import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.impl.platform.Services;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class DynamicAssetGeneratorClient {
    private DynamicAssetGeneratorClient() {}

    public static void init() {
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "texture"), TextureReader.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "fallback"), FallbackSource.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "combined_paletted_image"), CombinedPaletteImage.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "overlay"), Overlay.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask"), Mask.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "crop"), Crop.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "transform"), Transform.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "foreground_transfer"), ForegroundTransfer.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "color"), ColorSource.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "animation_splitter"), AnimationSplittingSource.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "frame_capture"), AnimationFrameCapture.CODEC);

        testing();
    }

    private static void testing() {
        //testing

        if (Services.PLATFORM.isDev()) {
            AssetResourceCache.INSTANCE.planSource(new DynamicTextureSource(new ResourceLocation("block/end_stone"),
                    new ForegroundTransfer(new TextureReader(new ResourceLocation("block/stone")),
                            new TextureReader(new ResourceLocation("block/redstone_ore")),
                            new TextureReader(new ResourceLocation("block/end_stone")),
                            6,true,true,true, 0.2d)));
            AssetResourceCache.INSTANCE.planSource(new DynamicTextureSource(new ResourceLocation("block/tuff"),
                    new ForegroundTransfer(new TextureReader(new ResourceLocation("block/stone")),
                            new TextureReader(new ResourceLocation("block/coal_ore")),
                            new TextureReader(new ResourceLocation("block/end_stone")),
                            6,true,true,true, 0.2d)));
            AssetResourceCache.INSTANCE.planSource(new DynamicTextureSource(new ResourceLocation("block/calcite"),
                    new ForegroundTransfer(new TextureReader(new ResourceLocation("block/stone")),
                            new TextureReader(new ResourceLocation("block/iron_ore")),
                            new TextureReader(new ResourceLocation("block/end_stone")),
                            6,true,true,true, 0.2d)));
            AssetResourceCache.INSTANCE.planSource(new DynamicTextureSource(new ResourceLocation("block/magma"),
                    new AnimationSplittingSource(Map.of("magma",
                            new AnimationSplittingSource.TimeAwareSource(new TextureReader(new ResourceLocation("block/magma")),1),
                            "prismarine",
                            new AnimationSplittingSource.TimeAwareSource(new TextureReader(new ResourceLocation("block/prismarine")),3)),
                            new CombinedPaletteImage(
                                    new TextureReader(new ResourceLocation("dynamic_asset_generator","empty")),
                                    new AnimationFrameCapture("prismarine"),
                                    new AnimationFrameCapture("magma"),
                                    false,
                                    true,
                                    6
                            ))));
        }
    }
}
