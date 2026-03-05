package dev.lukebemish.dynamicassetgenerator.impl.client;

import dev.lukebemish.dynamicassetgenerator.api.ResourceCache;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerator;
import dev.lukebemish.dynamicassetgenerator.api.client.AssetResourceCache;
import dev.lukebemish.dynamicassetgenerator.api.client.SpriteProvider;
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

    private static final AssetResourceCache ASSET_CACHE = ResourceCache.register(new BuiltinAssetResourceCache(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "builtin_assets")), Pack.Position.TOP);

    public static void init() {
        ResourceGenerator.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID,"texture"), TextureGenerator.CODEC);
        ResourceGenerator.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID,"texture_meta"), TextureMetaGenerator.CODEC);

        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "texture"), TextureReaderSource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "fallback"), FallbackSource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "palette_combined"), PaletteCombinedSource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "overlay"), OverlaySource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "mask"), MaskSource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "crop"), CropSource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "transform"), TransformSource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "foreground_transfer"), ForegroundTransferSource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "color"), ColorSource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "animation_splitter"), AnimationSplittingSource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "frame_capture"), AnimationFrameCapture.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "spread"), SpreadSource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "palette_spread"), PaletteSpreadSource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "error"), ErrorSource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "shadowed"), ShadowedSource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "channel_route"), ChannelRouteSource.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "tint"), TintSource.CODEC);

        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "mask/cutoff"), CutoffMask.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "mask/edge"), EdgeMask.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "mask/grow"), GrowMask.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "mask/invert"), InvertMask.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "mask/add"), AddMask.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "mask/multiply"), MultiplyMask.CODEC);
        TexSource.register(ResourceLocation.fromNamespaceAndPath(DynamicAssetGenerator.MOD_ID, "mask/channel"), ChannelMask.CODEC);

        SpriteProvider.register(BuiltinSpriteProvider.LOCATION, BuiltinSpriteProvider.CODEC);

        testing();
    }

    private static void testing() {
        //testing
        String test = System.getProperty("dynamicassetgenerator.test");
        if (test != null && test.equals("true")) {
            int paletteExtend = 6;
            ASSET_CACHE.planSource(new TextureGenerator(ResourceLocation.withDefaultNamespace("item/apple"),
                    new PaletteCombinedSource.Builder().setOverlay(new TextureReaderSource.Builder().setPath(ResourceLocation.parse("dynamic_asset_generator:empty")).build()).setBackground(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/stone")).build()).setPaletted(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("item/iron_ingot")).build()).setIncludeBackground(false).setStretchPaletted(false).setExtendPaletteSize(paletteExtend).build()));
            ASSET_CACHE.planSource(new TextureGenerator(ResourceLocation.withDefaultNamespace("item/carrot"),
                    new PaletteCombinedSource.Builder().setOverlay(new TextureReaderSource.Builder().setPath(ResourceLocation.parse("dynamic_asset_generator:empty")).build()).setBackground(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/stone")).build()).setPaletted(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("item/iron_ingot")).build()).setIncludeBackground(false).setStretchPaletted(false).setExtendPaletteSize(-1).build()));
            ASSET_CACHE.planSource(new TextureGenerator(ResourceLocation.withDefaultNamespace("block/end_stone"),
                    new ForegroundTransferSource.Builder().setBackground(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/stone")).build()).setFull(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/redstone_ore")).build()).setNewBackground(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/end_stone")).build()).build()));
            ASSET_CACHE.planSource(new TextureGenerator(ResourceLocation.withDefaultNamespace("block/diorite"),
                    new ForegroundTransferSource.Builder().setBackground(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/stone")).build()).setFull(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/coal_ore")).build()).setNewBackground(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/stone")).build()).build()));
            ASSET_CACHE.planSource(new TextureGenerator(ResourceLocation.withDefaultNamespace("block/tuff"),
                    new ForegroundTransferSource.Builder().setBackground(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/stone")).build()).setFull(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/coal_ore")).build()).setNewBackground(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/end_stone")).build()).build()));
            ASSET_CACHE.planSource(new TextureGenerator(ResourceLocation.withDefaultNamespace("block/calcite"),
                    new ForegroundTransferSource.Builder().setBackground(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/stone")).build()).setFull(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/iron_ore")).build()).setNewBackground(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/end_stone")).build()).build()));
            ASSET_CACHE.planSource(new TextureGenerator(ResourceLocation.withDefaultNamespace("block/andesite"),
                new ForegroundTransferSource.Builder().setBackground(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/stone")).build()).setFull(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/redstone_ore")).build()).setNewBackground(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/andesite")).build()).build()));
            ASSET_CACHE.planSource(new TextureGenerator(ResourceLocation.withDefaultNamespace("block/sand"),
                new ForegroundTransferSource.Builder().setBackground(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/stone")).build()).setFull(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/lapis_ore")).build()).setNewBackground(new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/end_stone")).build()).build()));
            ASSET_CACHE.planSource(new TextureGenerator(ResourceLocation.withDefaultNamespace("block/moss_block"),
                    new AnimationSplittingSource.Builder().setSources(Map.of("magma",
                        new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/magma")).build(),
                            "prismarine",
                        new TextureReaderSource.Builder().setPath(ResourceLocation.withDefaultNamespace("block/prismarine")).build())).setGenerator(new PaletteCombinedSource.Builder().setOverlay(new TextureReaderSource.Builder().setPath(AssetResourceCache.EMPTY_TEXTURE).build()).setBackground(new AnimationFrameCapture.Builder().setCapture("prismarine").build()).setPaletted(new AnimationFrameCapture.Builder().setCapture("magma").build()).setIncludeBackground(false).setStretchPaletted(true).setExtendPaletteSize(paletteExtend).build()).build()));
            ASSET_CACHE.planSource(new TextureMetaGenerator.Builder()
                .setSources(List.of(ResourceLocation.withDefaultNamespace("block/magma"), ResourceLocation.withDefaultNamespace("block/prismarine")))
                .setOutputLocation(ResourceLocation.withDefaultNamespace("block/moss_block"))
                .setAnimation(new TextureMetaGenerator.AnimationGenerator.Builder()
                    .build())
                .build()
            );
        }
    }
}
