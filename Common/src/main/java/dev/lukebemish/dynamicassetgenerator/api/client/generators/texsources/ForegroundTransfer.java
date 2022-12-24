/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.cache.CacheMetaJsonOps;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.client.PaletteExtractor;
import dev.lukebemish.dynamicassetgenerator.impl.client.palette.Palette;
import dev.lukebemish.dynamicassetgenerator.impl.client.util.IPalettePlan;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

public record ForegroundTransfer(ITexSource background, ITexSource full, ITexSource newBackground,
                                 int extendPaletteSize, boolean trimTrailing, boolean forceNeighbors, boolean fillHoles,
                                 double closeCutoff) implements ITexSource {
    public static final Codec<ForegroundTransfer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.CODEC.fieldOf("background").forGetter(ForegroundTransfer::background),
            ITexSource.CODEC.fieldOf("full").forGetter(ForegroundTransfer::full),
            ITexSource.CODEC.fieldOf("new_background").forGetter(ForegroundTransfer::newBackground),
            Codec.INT.fieldOf("extend_palette_size").forGetter(ForegroundTransfer::extendPaletteSize),
            Codec.BOOL.fieldOf("trim_trailing").forGetter(ForegroundTransfer::trimTrailing),
            Codec.BOOL.fieldOf("force_neighbors").forGetter(ForegroundTransfer::forceNeighbors),
            Codec.BOOL.fieldOf("fill_holes").forGetter(ForegroundTransfer::fillHoles),
            Codec.DOUBLE.fieldOf("close_cutoff").forGetter(ForegroundTransfer::closeCutoff)
    ).apply(instance, ForegroundTransfer::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> background = this.background().getSupplier(data, context);
        IoSupplier<NativeImage> newBackground = this.newBackground().getSupplier(data, context);
        IoSupplier<NativeImage> full = this.full().getSupplier(data, context);

        if (background == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.background());
            return null;
        }
        if (newBackground == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.newBackground());
            return null;
        }
        if (full == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.full());
            return null;
        }

        final DataResult<String> cacheKey;
        DataResult<String> cacheKey1;
        var dataOps = new CacheMetaJsonOps<>(data, TexSourceDataHolder.class);
        DataResult<String> cacheKeyBackground = ITexSource.CODEC.encodeStart(dataOps, background()).map(DynamicAssetGenerator.GSON_FLAT::toJson);
        DataResult<String> cacheKeyFull = ITexSource.CODEC.encodeStart(dataOps, full()).map(DynamicAssetGenerator.GSON_FLAT::toJson);
        if (cacheKeyBackground.result().isPresent() && cacheKeyFull.result().isPresent())
            cacheKey1 = DataResult.success(cacheKeyBackground.result().get() + "," + cacheKeyFull.result().get()+ "," + extendPaletteSize + "," + trimTrailing + "," + forceNeighbors + "," + fillHoles + "," + closeCutoff);
        else if (cacheKeyBackground.error().isPresent())
            cacheKey1 = DataResult.error("Failed to encode cache key: " + cacheKeyBackground.error().get().message());
        else
            cacheKey1 = DataResult.error("Failed to encode cache key: " + cacheKeyFull.error().get().message());
        cacheKey = cacheKey1;

        return () -> {
            try (NativeImage bImg = background.get();
                 NativeImage nImg = newBackground.get();
                 NativeImage fImg = full.get()) {

                try (PaletteExtractor extractor = new PaletteExtractor(context.cacheName(), cacheKey, bImg, fImg, this.extendPaletteSize(), this.trimTrailing(), this.forceNeighbors(), this.closeCutoff()).fillHoles(this.fillHoles())) {
                    PalettePlanner planner = PalettePlanner.of(extractor);
                    extractor.unCacheOrReCalc();
                    try (NativeImage pImg = extractor.getPalettedImg();
                         NativeImage oImg = extractor.getOverlayImg()) {
                        return Palette.paletteCombinedImage(nImg, oImg, pImg, planner);
                    }
                }
            }
        };
    }

    public static class PalettePlanner implements IPalettePlan {
        private final PaletteExtractor extractor;

        private PalettePlanner(PaletteExtractor extractor) {
            this.extractor = extractor;
        }

        public static PalettePlanner of(PaletteExtractor extractor) {
            return new PalettePlanner(extractor);
        }

        @Override
        public boolean includeBackground() {
            return true;
        }

        @Override
        public boolean stretchPaletted() {
            return false;
        }

        @Override
        public int extend() {
            return extractor.extend;
        }
    }
}
