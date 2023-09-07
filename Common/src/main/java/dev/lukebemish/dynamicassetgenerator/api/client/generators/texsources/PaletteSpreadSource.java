/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.api.client.image.ImageUtils;
import dev.lukebemish.dynamicassetgenerator.api.colors.ColorTypes;
import dev.lukebemish.dynamicassetgenerator.api.colors.Palette;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * A {@link TexSource} that maps the colors of another {@link TexSource} to the location of those colors within a
 * palette.
 */
@ApiStatus.Experimental
public class PaletteSpreadSource implements TexSource {
    public static final Codec<PaletteSpreadSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            TexSource.CODEC.fieldOf("source").forGetter(PaletteSpreadSource::getSource),
            Codec.DOUBLE.optionalFieldOf("palette_cutoff", Palette.DEFAULT_CUTOFF).forGetter(PaletteSpreadSource::getPaletteCutoff)
    ).apply(i, PaletteSpreadSource::new));

    private final TexSource source;
    private final double paletteCutoff;

    private PaletteSpreadSource(TexSource source, double paletteCutoff) {
        this.source = source;
        this.paletteCutoff = paletteCutoff;
    }

    @Override
    public @NotNull Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> source = getSource().getCachedSupplier(data, context);
        if (source == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.getSource().stringify());
            return null;
        }
        return () -> {
            try (NativeImage image = source.get()) {
                var palette = ImageUtils.getPalette(image, this.getPaletteCutoff());
                PointwiseOperation.Unary<Integer> operation = (c, i) -> {
                    if (!i) return 0;
                    var alpha = ColorTypes.ARGB32.alpha(c);
                    int sample = palette.getSample(c);
                    return FastColor.ARGB32.color(alpha, sample, sample, sample);
                };

                return ImageUtils.generateScaledImage(operation, List.of(image));
            }
        };
    }

    public TexSource getSource() {
        return source;
    }

    public double getPaletteCutoff() {
        return paletteCutoff;
    }

    public static class Builder {
        private TexSource source;
        private double paletteCutoff = Palette.DEFAULT_CUTOFF;

        /**
         * Sets the input texture to map the colors of.
         */
        public Builder setSource(TexSource source) {
            this.source = source;
            return this;
        }

        /**
         * Sets the cutoff for the palette. Colors within this Euclidean distance to each other, in integer RGB space,
         * will be considered identical. Defaults to 3.5.
         */
        public Builder setPaletteCutoff(double paletteCutoff) {
            this.paletteCutoff = paletteCutoff;
            return this;
        }

        public PaletteSpreadSource build() {
            Objects.requireNonNull(source);
            return new PaletteSpreadSource(source, paletteCutoff);
        }
    }
}
