/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client;

import dev.lukebemish.dynamicassetgenerator.api.ResourceCache;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerator;
import dev.lukebemish.dynamicassetgenerator.api.client.AssetResourceCache;
import dev.lukebemish.dynamicassetgenerator.api.client.DynamicSpriteSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TextureMetaGenerator;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TextureGenerator;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.*;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.mask.*;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;
import java.util.Map;

public class DynamicAssetGeneratorClient {
    private DynamicAssetGeneratorClient() {}

    private static final AssetResourceCache ASSET_CACHE = ResourceCache.register(new BuiltinAssetResourceCache(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "builtin_assets")), Pack.Position.TOP);

    public static void init() {
        ResourceGenerator.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID,"texture"), TextureGenerator.CODEC);
        ResourceGenerator.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID,"texture_meta"), TextureMetaGenerator.CODEC);

        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "texture"), TextureReaderSource.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "fallback"), FallbackSource.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "palette_combined"), PaletteCombinedSource.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "overlay"), OverlaySource.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask"), MaskSource.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "crop"), CropSource.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "transform"), TransformSource.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "foreground_transfer"), ForegroundTransferSource.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "color"), ColorSource.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "animation_splitter"), AnimationSplittingSource.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "frame_capture"), AnimationFrameCapture.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "palette_spread"), PaletteSpreadSource.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "error"), ErrorSource.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "shadowed"), ShadowedSource.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "channel_route"), ChannelRouteSource.CODEC);

        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask/cutoff"), CutoffMask.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask/edge"), EdgeMask.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask/grow"), GrowMask.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask/invert"), InvertMask.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask/add"), AddMask.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask/multiply"), MultiplyMask.CODEC);
        TexSource.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID, "mask/channel"), ChannelMask.CODEC);

        DynamicSpriteSource.register(BuiltinDynamicSpriteSource.LOCATION, BuiltinDynamicSpriteSource.CODEC);

        testing();
    }

    private static void testing() {
        //testing
        String test = System.getProperty("dynamicassetgenerator.test");
        if (test != null && test.equals("true")) {
            int paletteExtend = 6;
            ASSET_CACHE.planSource(new TextureGenerator(new ResourceLocation("item/apple"),
                    new PaletteCombinedSource.Builder().setOverlay(new TextureReaderSource.Builder().setPath(new ResourceLocation("dynamic_asset_generator:empty")).build()).setBackground(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/stone")).build()).setPaletted(new TextureReaderSource.Builder().setPath(new ResourceLocation("item/iron_ingot")).build()).setIncludeBackground(false).setStretchPaletted(false).setExtendPaletteSize(paletteExtend).build()));
            ASSET_CACHE.planSource(new TextureGenerator(new ResourceLocation("item/carrot"),
                    new PaletteCombinedSource.Builder().setOverlay(new TextureReaderSource.Builder().setPath(new ResourceLocation("dynamic_asset_generator:empty")).build()).setBackground(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/stone")).build()).setPaletted(new TextureReaderSource.Builder().setPath(new ResourceLocation("item/iron_ingot")).build()).setIncludeBackground(false).setStretchPaletted(false).setExtendPaletteSize(-1).build()));
            ASSET_CACHE.planSource(new TextureGenerator(new ResourceLocation("block/end_stone"),
                    new ForegroundTransferSource.Builder().setBackground(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/stone")).build()).setFull(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/redstone_ore")).build()).setNewBackground(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/end_stone")).build()).build()));
            ASSET_CACHE.planSource(new TextureGenerator(new ResourceLocation("block/diorite"),
                    new ForegroundTransferSource.Builder().setBackground(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/stone")).build()).setFull(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/coal_ore")).build()).setNewBackground(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/stone")).build()).build()));
            ASSET_CACHE.planSource(new TextureGenerator(new ResourceLocation("block/tuff"),
                    new ForegroundTransferSource.Builder().setBackground(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/stone")).build()).setFull(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/coal_ore")).build()).setNewBackground(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/end_stone")).build()).build()));
            ASSET_CACHE.planSource(new TextureGenerator(new ResourceLocation("block/calcite"),
                    new ForegroundTransferSource.Builder().setBackground(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/stone")).build()).setFull(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/iron_ore")).build()).setNewBackground(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/end_stone")).build()).build()));
            ASSET_CACHE.planSource(new TextureGenerator(new ResourceLocation("block/andesite"),
                new ForegroundTransferSource.Builder().setBackground(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/stone")).build()).setFull(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/redstone_ore")).build()).setNewBackground(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/andesite")).build()).build()));
            ASSET_CACHE.planSource(new TextureGenerator(new ResourceLocation("block/sand"),
                new ForegroundTransferSource.Builder().setBackground(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/stone")).build()).setFull(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/lapis_ore")).build()).setNewBackground(new TextureReaderSource.Builder().setPath(new ResourceLocation("block/end_stone")).build()).build()));
            ASSET_CACHE.planSource(new TextureGenerator(new ResourceLocation("block/moss_block"),
                    new AnimationSplittingSource.Builder().setSources(Map.of("magma",
                        new TextureReaderSource.Builder().setPath(new ResourceLocation("block/magma")).build(),
                            "prismarine",
                        new TextureReaderSource.Builder().setPath(new ResourceLocation("block/prismarine")).build())).setGenerator(new PaletteCombinedSource.Builder().setOverlay(new TextureReaderSource.Builder().setPath(new ResourceLocation("dynamic_asset_generator", "empty")).build()).setBackground(new AnimationFrameCapture.Builder().setCapture("prismarine").build()).setPaletted(new AnimationFrameCapture.Builder().setCapture("magma").build()).setIncludeBackground(false).setStretchPaletted(true).setExtendPaletteSize(paletteExtend).build()).build()));
            ASSET_CACHE.planSource(new TextureMetaGenerator.Builder()
                .setSources(List.of(new ResourceLocation("block/magma"), new ResourceLocation("block/prismarine")))
                .setOutputLocation(new ResourceLocation("block/moss_block"))
                .setAnimation(new TextureMetaGenerator.AnimationGenerator.Builder()
                    .build())
                .build()
            );
        }
    }
}
