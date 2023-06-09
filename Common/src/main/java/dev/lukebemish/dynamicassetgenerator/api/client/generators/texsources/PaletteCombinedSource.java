/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
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
import dev.lukebemish.dynamicassetgenerator.api.colors.Palette;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.ColorOperations;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PaletteToColorOperation;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class PaletteCombinedSource implements TexSource {
    private static final boolean DEFAULT_INCLUDE_BACKGROUND = true;
    private static final boolean DEFAULT_STRETCH_PALETTED = false;
    private static final int DEFAULT_EXTEND_PALETTE_SIZE = 6;

    public static final Codec<PaletteCombinedSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TexSource.CODEC.fieldOf("overlay").forGetter(PaletteCombinedSource::getOverlay),
            TexSource.CODEC.fieldOf("background").forGetter(PaletteCombinedSource::getBackground),
            TexSource.CODEC.fieldOf("paletted").forGetter(PaletteCombinedSource::getPaletted),
            Codec.BOOL.optionalFieldOf("include_background", DEFAULT_INCLUDE_BACKGROUND).forGetter(PaletteCombinedSource::isIncludeBackground),
            Codec.BOOL.optionalFieldOf("stretch_paletted", DEFAULT_STRETCH_PALETTED).forGetter(PaletteCombinedSource::isStretchPaletted),
            Codec.INT.optionalFieldOf("extend_palette_size", DEFAULT_EXTEND_PALETTE_SIZE).forGetter(PaletteCombinedSource::getExtendPaletteSize)
    ).apply(instance, PaletteCombinedSource::new));
    private final TexSource overlay;
    private final TexSource background;
    private final TexSource paletted;
    private final boolean includeBackground;
    private final boolean stretchPaletted;
    private final int extendPaletteSize;

    private PaletteCombinedSource(TexSource overlay, TexSource background, TexSource paletted, boolean includeBackground, boolean stretchPaletted, int extendPaletteSize) {
        this.overlay = overlay;
        this.background = background;
        this.paletted = paletted;
        this.includeBackground = includeBackground;
        this.stretchPaletted = stretchPaletted;
        this.extendPaletteSize = extendPaletteSize;
    }

    @Override
    public Codec<PaletteCombinedSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        //PalettePlanner planner = PalettePlanner.of(this, data, context);
        var backgroundSupplier = this.background.getSupplier(data, context);
        var overlaySupplier = this.overlay.getSupplier(data, context);
        var palettedSupplier = this.paletted.getSupplier(data, context);
        if (backgroundSupplier == null) {
            data.getLogger().error("Background image was none... \n{}", background.stringify());
            return null;
        }
        if (overlaySupplier == null) {
            data.getLogger().error("Overlay image was none... \n{}", overlay.stringify());
            return null;
        }
        if (palettedSupplier == null) {
            data.getLogger().error("Paletted image was none... \n{}", paletted.stringify());
            return null;
        }
        return () -> {
            try (NativeImage bImg = backgroundSupplier.get();
                 NativeImage oImg = overlaySupplier.get();
                 NativeImage pImg = palettedSupplier.get()) {

                return combineImages(bImg, oImg, pImg, new PaletteCombiningOptions(palette -> palette.size() >= extendPaletteSize, stretchPaletted, includeBackground));
            }
        };
    }

    @NotNull
    public static NativeImage combineImages(NativeImage backgroundImage, NativeImage overlayImage, NativeImage paletteImage, PaletteCombiningOptions options) {
        Palette palette = ImageUtils.getPalette(backgroundImage);
        palette.extend(options.palettePredicate());
        final PointwiseOperation.Unary<Integer> stretcher;
        if (options.stretchPaletted()) {
            int min = 0xFF;
            int max = 0x00;
            for (int i = 0; i < paletteImage.getWidth(); i++) {
                for (int j = 0; j < paletteImage.getHeight(); j++) {
                    int color = paletteImage.getPixelRGBA(i, j);
                    int value = (FastColor.ABGR32.red(color) + FastColor.ABGR32.green(color) + FastColor.ABGR32.blue(color)) / 3;
                    if (value < min)
                        min = value;
                    if (value > max)
                        max = value;
                }
            }
            int finalMax = max;
            int finalMin = min;
            stretcher = (color, isInBounds) -> {
                int value = (FastColor.ARGB32.red(color) + FastColor.ARGB32.green(color) + FastColor.ARGB32.blue(color)) / 3;
                int stretched = (value - finalMin) * 255 / (finalMax - finalMin);
                return FastColor.ARGB32.color(FastColor.ARGB32.alpha(color), stretched, stretched, stretched);
            };
        } else {
            stretcher = (color, isInBounds) -> color;
        }

        final PointwiseOperation.Unary<Integer> paletteResolver = new PaletteToColorOperation(palette);

        PointwiseOperation.Ternary<Integer> operation = (background,
                                                         overlay,
                                                         paletted,
                                                         backgroundInBounds,
                                                         overlayInBounds,
                                                         palettedInBounds) -> {
            paletted = stretcher.apply(paletted, palettedInBounds);

            int resolvedPalette = paletteResolver.apply(paletted, palettedInBounds);
            int[] toOverlay = options.includeBackground() ? new int[]{overlay, resolvedPalette, background} : new int[]{overlay, resolvedPalette};
            boolean[] toOverlayInBounds = options.includeBackground() ? new boolean[]{overlayInBounds, palettedInBounds, backgroundInBounds} : new boolean[]{overlayInBounds, palettedInBounds};
            return ColorOperations.OVERLAY.apply(toOverlay, toOverlayInBounds);
        };

        return ImageUtils.generateScaledImage(operation, List.of(backgroundImage, overlayImage, paletteImage));
    }

    public TexSource getOverlay() {
        return overlay;
    }

    public TexSource getBackground() {
        return background;
    }

    public TexSource getPaletted() {
        return paletted;
    }

    public boolean isIncludeBackground() {
        return includeBackground;
    }

    public boolean isStretchPaletted() {
        return stretchPaletted;
    }

    public int getExtendPaletteSize() {
        return extendPaletteSize;
    }


    public record PaletteCombiningOptions(Predicate<Palette> palettePredicate, boolean stretchPaletted,
                                          boolean includeBackground) {
    }

    public static class Builder {
        private TexSource overlay;
        private TexSource background;
        private TexSource paletted;
        private boolean includeBackground = DEFAULT_INCLUDE_BACKGROUND;
        private boolean stretchPaletted = DEFAULT_STRETCH_PALETTED;
        private int extendPaletteSize = DEFAULT_EXTEND_PALETTE_SIZE;

        public Builder setOverlay(TexSource overlay) {
            this.overlay = overlay;
            return this;
        }

        public Builder setBackground(TexSource background) {
            this.background = background;
            return this;
        }

        public Builder setPaletted(TexSource paletted) {
            this.paletted = paletted;
            return this;
        }

        public Builder setIncludeBackground(boolean includeBackground) {
            this.includeBackground = includeBackground;
            return this;
        }

        public Builder setStretchPaletted(boolean stretchPaletted) {
            this.stretchPaletted = stretchPaletted;
            return this;
        }

        public Builder setExtendPaletteSize(int extendPaletteSize) {
            this.extendPaletteSize = extendPaletteSize;
            return this;
        }

        public PaletteCombinedSource build() {
            Objects.requireNonNull(overlay);
            Objects.requireNonNull(background);
            Objects.requireNonNull(paletted);
            return new PaletteCombinedSource(overlay, background, paletted, includeBackground, stretchPaletted, extendPaletteSize);
        }
    }
}
