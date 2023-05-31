/*
 * Copyright (C) 2022 Luke Bemish and contributors
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
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.*;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public record CombinedPaletteImage(TexSource overlay, TexSource background, TexSource paletted, boolean includeBackground, boolean stretchPaletted, int extendPaletteSize) implements TexSource {
    public static final Codec<CombinedPaletteImage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TexSource.CODEC.fieldOf("overlay").forGetter(s->s.overlay),
            TexSource.CODEC.fieldOf("background").forGetter(s->s.background),
            TexSource.CODEC.fieldOf("paletted").forGetter(s->s.paletted),
            Codec.BOOL.fieldOf("include_background").forGetter(s->s.includeBackground),
            Codec.BOOL.fieldOf("stretch_paletted").forGetter(s->s.stretchPaletted),
            Codec.INT.fieldOf("extend_palette_size").forGetter(s->s.extendPaletteSize)
    ).apply(instance,CombinedPaletteImage::new));

    @Override
    public Codec<CombinedPaletteImage> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        //PalettePlanner planner = PalettePlanner.of(this, data, context);
        var backgroundSupplier = this.background.getSupplier(data, context);
        var overlaySupplier = this.overlay.getSupplier(data, context);
        var palettedSupplier = this.paletted.getSupplier(data, context);
        if (backgroundSupplier == null) {
            data.getLogger().error("Background image was none... \n{}", background);
            return null;
        }
        if (overlaySupplier == null) {
            data.getLogger().error("Overlay image was none... \n{}", overlay);
            return null;
        }
        if (palettedSupplier == null) {
            data.getLogger().error("Paletted image was none... \n{}", paletted);
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
                    int value = (FastColor.ABGR32.red(color) + FastColor.ABGR32.green(color) + FastColor.ABGR32.blue(color))/3;
                    if (value < min)
                        min = value;
                    if (value > max)
                        max = value;
                }
            }
            int finalMax = max;
            int finalMin = min;
            stretcher = (color, isInBounds) -> {
                int value = (FastColor.ARGB32.red(color) + FastColor.ARGB32.green(color) + FastColor.ARGB32.blue(color))/3;
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

    public record PaletteCombiningOptions(Predicate<Palette> palettePredicate, boolean stretchPaletted, boolean includeBackground) { }
}
