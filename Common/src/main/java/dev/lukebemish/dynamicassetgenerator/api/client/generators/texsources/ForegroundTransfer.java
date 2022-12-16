/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
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

        String cacheKey;
        try {
            String cacheKeyBackground = DynamicAssetGenerator.GSON_FLAT.toJson(ITexSource.CODEC.encodeStart(JsonOps.INSTANCE, background()).getOrThrow(false, e->{}));
            String cacheKeyFull = DynamicAssetGenerator.GSON_FLAT.toJson(ITexSource.CODEC.encodeStart(JsonOps.INSTANCE, full()).getOrThrow(false, e->{}));
            cacheKey = cacheKeyBackground + cacheKeyFull + "_" + extendPaletteSize + "_" + trimTrailing + "_" + forceNeighbors + "_" + fillHoles + "_" + closeCutoff;
        } catch (RuntimeException e) {
            data.getLogger().error("Failed to encode texture source to JSON for caching", e);
            return null;
        }

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
