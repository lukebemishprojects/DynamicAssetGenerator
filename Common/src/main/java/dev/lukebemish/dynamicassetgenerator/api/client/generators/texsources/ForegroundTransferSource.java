/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.cache.CacheMetaJsonOps;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.api.colors.Palette;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.client.ForegroundExtractor;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

public final class ForegroundTransferSource implements TexSource {
    private static final int DEFAULT_EXTEND_PALETTE_SIZE = 6;
    private static final boolean DEFAULT_TRIM_TRAILING = true;
    private static final boolean DEFAULT_FORCE_NEIGHBORS = true;
    private static final boolean DEFAULT_FILL_HOLES = true;
    private static final double DEFAULT_CLOSE_CUTOFF = 2;

    public static final Codec<ForegroundTransferSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TexSource.CODEC.fieldOf("background").forGetter(ForegroundTransferSource::getBackground),
            TexSource.CODEC.fieldOf("full").forGetter(ForegroundTransferSource::getFull),
            TexSource.CODEC.fieldOf("new_background").forGetter(ForegroundTransferSource::getNewBackground),
            Codec.INT.optionalFieldOf("extend_palette_size", DEFAULT_EXTEND_PALETTE_SIZE).forGetter(ForegroundTransferSource::getExtendPaletteSize),
            Codec.BOOL.optionalFieldOf("trim_trailing", DEFAULT_TRIM_TRAILING).forGetter(ForegroundTransferSource::isTrimTrailing),
            Codec.BOOL.optionalFieldOf("force_neighbors", DEFAULT_FORCE_NEIGHBORS).forGetter(ForegroundTransferSource::isForceNeighbors),
            Codec.BOOL.optionalFieldOf("fill_holes", DEFAULT_FILL_HOLES).forGetter(ForegroundTransferSource::isFillHoles),
            Codec.DOUBLE.optionalFieldOf("close_cutoff", DEFAULT_CLOSE_CUTOFF).forGetter(ForegroundTransferSource::getCloseCutoff)
    ).apply(instance, ForegroundTransferSource::new));
    private final TexSource background;
    private final TexSource full;
    private final TexSource newBackground;
    private final int extendPaletteSize;
    private final boolean trimTrailing;
    private final boolean forceNeighbors;
    private final boolean fillHoles;
    private final double closeCutoff;

    private ForegroundTransferSource(TexSource background, TexSource full, TexSource newBackground,
                                     int extendPaletteSize, boolean trimTrailing, boolean forceNeighbors, boolean fillHoles,
                                     double closeCutoff) {
        this.background = background;
        this.full = full;
        this.newBackground = newBackground;
        this.extendPaletteSize = extendPaletteSize;
        this.trimTrailing = trimTrailing;
        this.forceNeighbors = forceNeighbors;
        this.fillHoles = fillHoles;
        this.closeCutoff = closeCutoff;
    }

    @Override
    public Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> background = this.getBackground().getSupplier(data, context);
        IoSupplier<NativeImage> newBackground = this.getNewBackground().getSupplier(data, context);
        IoSupplier<NativeImage> full = this.getFull().getSupplier(data, context);

        if (background == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.getBackground().stringify());
            return null;
        }
        if (newBackground == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.getNewBackground().stringify());
            return null;
        }
        if (full == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.getFull().stringify());
            return null;
        }

        final DataResult<String> cacheKey;
        DataResult<String> cacheKey1;
        var dataOps = new CacheMetaJsonOps();
        dataOps.putData(TexSourceDataHolder.class, data);
        DataResult<String> cacheKeyBackground = TexSource.CODEC.encodeStart(dataOps, getBackground()).map(DynamicAssetGenerator.GSON_FLAT::toJson);
        DataResult<String> cacheKeyFull = TexSource.CODEC.encodeStart(dataOps, getFull()).map(DynamicAssetGenerator.GSON_FLAT::toJson);
        if (cacheKeyBackground.result().isPresent() && cacheKeyFull.result().isPresent())
            cacheKey1 = DataResult.success(cacheKeyBackground.result().get() + "," + cacheKeyFull.result().get() + "," + extendPaletteSize + "," + trimTrailing + "," + forceNeighbors + "," + fillHoles + "," + closeCutoff);
        else if (cacheKeyBackground.error().isPresent())
            cacheKey1 = DataResult.error(() -> "Failed to encode cache key: " + cacheKeyBackground.error().get().message());
        else
            cacheKey1 = DataResult.error(() -> "Failed to encode cache key: " + cacheKeyFull.error().get().message());
        cacheKey = cacheKey1;

        return () -> {
            try (NativeImage bImg = background.get();
                 NativeImage nImg = newBackground.get();
                 NativeImage fImg = full.get()) {

                Predicate<Palette> extend = p -> p.size() >= extendPaletteSize;
                try (ForegroundExtractor extractor = new ForegroundExtractor(context.getCacheName(), cacheKey, bImg, fImg, extend, this.isTrimTrailing(), this.isForceNeighbors(), this.getCloseCutoff()).fillHoles(this.isFillHoles())) {
                    var options = new PaletteCombinedSource.PaletteCombiningOptions(extend, false, true);
                    extractor.unCacheOrReCalc();
                    try (NativeImage pImg = extractor.getPalettedImg();
                         NativeImage oImg = extractor.getOverlayImg()) {
                        return PaletteCombinedSource.combineImages(nImg, oImg, pImg, options);
                    }
                }
            }
        };
    }

    public TexSource getBackground() {
        return background;
    }

    public TexSource getFull() {
        return full;
    }

    public TexSource getNewBackground() {
        return newBackground;
    }

    public int getExtendPaletteSize() {
        return extendPaletteSize;
    }

    public boolean isTrimTrailing() {
        return trimTrailing;
    }

    public boolean isForceNeighbors() {
        return forceNeighbors;
    }

    public boolean isFillHoles() {
        return fillHoles;
    }

    public double getCloseCutoff() {
        return closeCutoff;
    }

    public static class Builder {
        private TexSource background;
        private TexSource full;
        private TexSource newBackground;
        private int extendPaletteSize = DEFAULT_EXTEND_PALETTE_SIZE;
        private boolean trimTrailing = DEFAULT_TRIM_TRAILING;
        private boolean forceNeighbors = DEFAULT_FORCE_NEIGHBORS;
        private boolean fillHoles = DEFAULT_FILL_HOLES;
        private double closeCutoff = DEFAULT_CLOSE_CUTOFF;

        public Builder setBackground(TexSource background) {
            this.background = background;
            return this;
        }

        public Builder setFull(TexSource full) {
            this.full = full;
            return this;
        }

        public Builder setNewBackground(TexSource newBackground) {
            this.newBackground = newBackground;
            return this;
        }

        public Builder setExtendPaletteSize(int extendPaletteSize) {
            this.extendPaletteSize = extendPaletteSize;
            return this;
        }

        public Builder setTrimTrailing(boolean trimTrailing) {
            this.trimTrailing = trimTrailing;
            return this;
        }

        public Builder setForceNeighbors(boolean forceNeighbors) {
            this.forceNeighbors = forceNeighbors;
            return this;
        }

        public Builder setFillHoles(boolean fillHoles) {
            this.fillHoles = fillHoles;
            return this;
        }

        public Builder setCloseCutoff(double closeCutoff) {
            this.closeCutoff = closeCutoff;
            return this;
        }

        public ForegroundTransferSource build() {
            Objects.requireNonNull(background);
            Objects.requireNonNull(full);
            Objects.requireNonNull(newBackground);
            return new ForegroundTransferSource(background, full, newBackground, extendPaletteSize, trimTrailing, forceNeighbors, fillHoles, closeCutoff);
        }
    }
}
