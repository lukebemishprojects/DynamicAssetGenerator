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
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * A {@link TexSource} that overlays one image on top of another, adding a directional shadow around the edges of the
 * upper image, using the palette of the lower image.
 */
public class ShadowedSource implements TexSource {
    private static final int DEFAULT_EXTEND_PALETTE_SIZE = 6;
    private static final int DEFAULT_HIGHLIGHT_STRENGTH = 72;
    private static final int DEFAULT_SHADOW_STRENGTH = 72;
    private static final float DEFAULT_UNIFORMITY = 1.0f;

    private static final int[] LOW_X = new int[] {1,0};
    private static final int[] LOW_Y = new int[] {0,1};
    private static final int[] HIGH_X = new int[] {-1,0};
    private static final int[] HIGH_Y = new int[] {0,-1};

    public static final Codec<ShadowedSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            TexSource.CODEC.fieldOf("background").forGetter(ShadowedSource::getBackground),
            TexSource.CODEC.fieldOf("foreground").forGetter(ShadowedSource::getForeground),
            Codec.INT.optionalFieldOf("extend_palette_size", DEFAULT_EXTEND_PALETTE_SIZE).forGetter(ShadowedSource::getExtendPaletteSize),
            Codec.INT.optionalFieldOf("highlight_strength", DEFAULT_HIGHLIGHT_STRENGTH).forGetter(ShadowedSource::getHighlightStrength),
            Codec.INT.optionalFieldOf("shadow_strength", DEFAULT_SHADOW_STRENGTH).forGetter(ShadowedSource::getShadowStrength),
            Codec.FLOAT.optionalFieldOf("uniformity", DEFAULT_UNIFORMITY).forGetter(ShadowedSource::getUniformity)
    ).apply(i, ShadowedSource::new));


    private final TexSource background;
    private final TexSource foreground;
    private final int extendPaletteSize;
    private final int highlightStrength;
    private final int shadowStrength;
    private final float uniformity;

    private ShadowedSource(TexSource background, TexSource foreground, int extendPaletteSize, int highlightStrength, int shadowStrength, float uniformity) {
        this.background = background;
        this.foreground = foreground;
        this.extendPaletteSize = extendPaletteSize;
        this.highlightStrength = highlightStrength;
        this.shadowStrength = shadowStrength;
        this.uniformity = uniformity;
    }

    @Override
    public Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> background = this.getBackground().getCachedSupplier(data, context);
        IoSupplier<NativeImage> foreground = this.getForeground().getCachedSupplier(data, context);

        if (background == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.getBackground().stringify());
            return null;
        }
        if (foreground == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.getForeground().stringify());
            return null;
        }

        return () -> {
            try (NativeImage backgroundImage = background.get();
                 NativeImage foregroundImage = foreground.get()) {
                var size = ImageUtils.calculateScaledSize(List.of(backgroundImage, foregroundImage));
                int bScale = size.getFirst() / backgroundImage.getWidth();
                int fScale = size.getFirst() / foregroundImage.getWidth();
                var image = new NativeImage(size.getFirst(), size.getSecond(), false);
                Palette palette = ImageUtils.getPalette(backgroundImage);
                palette.extendToSize(this.getExtendPaletteSize());
                IntStream.range(0, size.getFirst()).parallel().forEach(x -> {
                    for (int y = 0; y < size.getSecond(); y++) {
                        ImageUtils.safeSetPixelABGR(image, x, y, 0);
                        boolean high = false;
                        boolean low = false;

                        for (int i = 0; i < LOW_X.length; i++) {
                            if (FastColor.ABGR32.alpha(ImageUtils.safeGetPixelABGR(foregroundImage, (x + LOW_X[i])/fScale, (y + LOW_Y[i])/fScale)) >= 128) {
                                low = true;
                                break;
                            }
                        }
                        for (int i = 0; i < HIGH_X.length; i++) {
                            if (FastColor.ABGR32.alpha(ImageUtils.safeGetPixelABGR(foregroundImage, (x + HIGH_X[i])/fScale, (y + HIGH_Y[i])/fScale)) >= 128) {
                                high = true;
                                break;
                            }
                        }

                        if (high && low) {
                            high = false;
                            low = false;
                        }

                        int oldBackground = ImageUtils.safeGetPixelARGB(backgroundImage, x/bScale, y/bScale);
                        int oldForeground = ImageUtils.safeGetPixelARGB(foregroundImage, x/fScale, y/fScale);

                        if (high || low) {
                            int sample = palette.getSample(oldBackground);
                            sample = (int) ((sample + palette.originalCenterSample() * this.getUniformity()) / (1 + this.getUniformity()));

                            if (high)
                                sample += this.getHighlightStrength();
                            else
                                sample -= this.getShadowStrength();

                            sample = ColorTypes.clamp8(sample);

                            int newColor = palette.getColor(sample) | (oldBackground & 0xFF000000);
                            ImageUtils.safeSetPixelARGB(image, x, y, ColorTypes.ARGB32.alphaBlend(oldForeground, newColor));
                        } else {
                            ImageUtils.safeSetPixelARGB(image, x, y, ColorTypes.ARGB32.alphaBlend(oldForeground, oldBackground));
                        }
                    }
                });
                return image;
            }
        };
    }

    public float getUniformity() {
        return uniformity;
    }

    public int getShadowStrength() {
        return shadowStrength;
    }

    public int getHighlightStrength() {
        return highlightStrength;
    }

    public int getExtendPaletteSize() {
        return extendPaletteSize;
    }

    public TexSource getForeground() {
        return foreground;
    }

    public TexSource getBackground() {
        return background;
    }

    public static class Builder {
        private TexSource background;
        private TexSource foreground;
        private int extendPaletteSize = DEFAULT_EXTEND_PALETTE_SIZE;
        private int highlightStrength = DEFAULT_HIGHLIGHT_STRENGTH;
        private int shadowStrength = DEFAULT_SHADOW_STRENGTH;
        private float uniformity = DEFAULT_UNIFORMITY;

        /**
         * Sets the background image to be overlayed onto.
         */
        public Builder setBackground(TexSource background) {
            this.background = background;
            return this;
        }

        /**
         * Sets the image to overlay onto the background image.
         */
        public Builder setForeground(TexSource foreground) {
            this.foreground = foreground;
            return this;
        }

        /**
         * Sets a minimum size that the palette of the background image will be extended to if it is not already this
         * size. Defaults to 6.
         */
        public Builder setExtendPaletteSize(int extendPaletteSize) {
            this.extendPaletteSize = extendPaletteSize;
            return this;
        }

        /**
         * Sets how strong the generated highlight should be, on a relative scale. Defaults to 72.
         */
        public Builder setHighlightStrength(int highlightStrength) {
            this.highlightStrength = highlightStrength;
            return this;
        }

        /**
         * Sets how strong the generated shadow should be, on a relative scale. Defaults to 72.
         */
        public Builder setShadowStrength(int shadowStrength) {
            this.shadowStrength = shadowStrength;
            return this;
        }

        /**
         * Sets how uniform the generated highlight and shadow should be. 0 is as uniform as the original image, and
         * the uniformity increases as this value increases, becoming completely uniform as this value becomes extremely
         * large. Defaults to 1.0.
         */
        public Builder setUniformity(float uniformity) {
            this.uniformity = uniformity;
            return this;
        }

        public ShadowedSource build() {
            Objects.requireNonNull(background);
            Objects.requireNonNull(foreground);
            return new ShadowedSource(background, foreground, extendPaletteSize, highlightStrength, shadowStrength, uniformity);
        }
    }
}
