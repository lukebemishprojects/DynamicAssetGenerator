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

                //TODO: figure out where the cutoff comes from and what to use for it
                return combineImages(bImg, oImg, pImg, new PaletteCombiningOptions(palette -> palette.size() >= extendPaletteSize, stretchPaletted, includeBackground));
            }
        };
    }

    @NotNull
    public static NativeImage combineImages(NativeImage backgroundImage, NativeImage overlayImage, NativeImage paletteImage, PaletteCombiningOptions options) {
        //TODO: Figure out this cutoff
        Palette palette = ImageUtils.getPalette(backgroundImage, 2);
        palette.extend(options.palettePredicate());
        final PointwiseOperation.Unary<Integer> stretcher;
        if (options.stretchPaletted()) {
            Palette palettePalette = ImageUtils.getPalette(paletteImage, 2);
            stretcher = new ColorToPaletteOperation(palettePalette);
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
            return Operations.OVERLAY.apply(toOverlay, toOverlayInBounds);
        };

        return ImageUtils.generateScaledImage(operation, List.of(backgroundImage, overlayImage, paletteImage));
    }

    public record PaletteCombiningOptions(Predicate<Palette> palettePredicate, boolean stretchPaletted, boolean includeBackground) { }
}
