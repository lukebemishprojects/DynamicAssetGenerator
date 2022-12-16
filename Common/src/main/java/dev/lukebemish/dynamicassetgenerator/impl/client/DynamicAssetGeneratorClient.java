/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client;

import dev.lukebemish.dynamicassetgenerator.api.IResourceGenerator;
import dev.lukebemish.dynamicassetgenerator.api.ResourceCache;
import dev.lukebemish.dynamicassetgenerator.api.client.AssetResourceCache;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TextureGenerator;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TextureMetaGenerator;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.*;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.mask.*;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DynamicAssetGeneratorClient {
    private DynamicAssetGeneratorClient() {}

    private static final AssetResourceCache ASSET_CACHE = ResourceCache.register(new BuiltinAssetResourceCache(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "builtin_assets")), Pack.Position.TOP);

    public static void init() {
        IResourceGenerator.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID,"texture"), TextureGenerator.CODEC);
        IResourceGenerator.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID,"texture_meta"), TextureMetaGenerator.CODEC);

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
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "palette_spread"), PaletteSpreadSource.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "error"), ErrorSource.CODEC);

        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask/cutoff"), CutoffMask.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask/edge"), EdgeMask.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask/grow"), GrowMask.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask/invert"), InvertMask.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask/add"), AddMask.CODEC);
        ITexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask/multiply"), MultiplyMask.CODEC);



        testing();
    }

    private static void testing() {
        //testing
        String test = System.getProperty("dynamicassetgenerator.test");
        if (test != null && test.equals("true")) {
            ASSET_CACHE.planSource(new TextureGenerator(new ResourceLocation("block/end_stone"),
                    new ForegroundTransfer(new TextureReader(new ResourceLocation("block/stone")),
                            new TextureReader(new ResourceLocation("block/redstone_ore")),
                            new TextureReader(new ResourceLocation("block/end_stone")),
                            6,true,true,true, 0.2d)));
            ASSET_CACHE.planSource(new TextureGenerator(new ResourceLocation("block/tuff"),
                    new ForegroundTransfer(new TextureReader(new ResourceLocation("block/stone")),
                            new TextureReader(new ResourceLocation("block/coal_ore")),
                            new TextureReader(new ResourceLocation("block/end_stone")),
                            6,true,true,true, 0.2d)));
            ASSET_CACHE.planSource(new TextureGenerator(new ResourceLocation("block/calcite"),
                    new ForegroundTransfer(new TextureReader(new ResourceLocation("block/stone")),
                            new TextureReader(new ResourceLocation("block/iron_ore")),
                            new TextureReader(new ResourceLocation("block/end_stone")),
                            6,true,true,true, 0.2d)));
            ASSET_CACHE.planSource(new TextureGenerator(new ResourceLocation("block/andesite"),
                    new ForegroundTransfer(new TextureReader(new ResourceLocation("block/stone")),
                            new TextureReader(new ResourceLocation("block/redstone_ore")),
                            new TextureReader(new ResourceLocation("block/andesite")),
                            6,true,true,true, 0.2d)));
            ASSET_CACHE.planSource(new TextureGenerator(new ResourceLocation("block/magma"),
                    new AnimationSplittingSource(Map.of("magma",
                            new AnimationSplittingSource.TimeAwareSource(new TextureReader(new ResourceLocation("block/magma")),1),
                            "prismarine",
                            new AnimationSplittingSource.TimeAwareSource(new TextureReader(new ResourceLocation("block/prismarine")),4)),
                            new CombinedPaletteImage(
                                    new TextureReader(new ResourceLocation("dynamic_asset_generator","empty")),
                                    new AnimationFrameCapture("prismarine"),
                                    new AnimationFrameCapture("magma"),
                                    false,
                                    true,
                                    6
                            ))));
            ASSET_CACHE.planSource(new TextureMetaGenerator(List.of(new ResourceLocation("block/magma"),new ResourceLocation("block/prismarine")),
                    Optional.of(new TextureMetaGenerator.AnimationData(Optional.empty(), Optional.empty(),Optional.of(new ResourceLocation("block/prismarine")),
                            Optional.of(List.of(1,4)))),
                    Optional.empty(),
                    Optional.empty(),
                    new ResourceLocation("block/magma")));
        }
    }
}
