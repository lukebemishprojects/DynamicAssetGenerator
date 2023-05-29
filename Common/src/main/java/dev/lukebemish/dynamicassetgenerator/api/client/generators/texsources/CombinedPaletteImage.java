/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.api.client.image.ImageUtils;
import dev.lukebemish.dynamicassetgenerator.api.colors.Palette;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.*;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record CombinedPaletteImage(ITexSource overlay, ITexSource background, ITexSource paletted, boolean includeBackground, boolean stretchPaletted, int extendPaletteSize) implements ITexSource {
    public static final Codec<CombinedPaletteImage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.CODEC.fieldOf("overlay").forGetter(s->s.overlay),
            ITexSource.CODEC.fieldOf("background").forGetter(s->s.background),
            ITexSource.CODEC.fieldOf("paletted").forGetter(s->s.paletted),
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
                Palette palette = ImageUtils.getPalette(bImg, 2);
                palette.extendToSize(extendPaletteSize);
                final PointwiseOperation.UnaryPointwiseOperation<Integer> stretcher;
                if (stretchPaletted) {
                    Palette palettePalette = ImageUtils.getPalette(pImg, 2);
                    stretcher = new ColorToPaletteOperation(palettePalette);
                } else {
                    stretcher = (color, isInBounds) -> color;
                }

                final PointwiseOperation.UnaryPointwiseOperation<Integer> paletteResolver = new PaletteToColorOperation(palette);

                PointwiseOperation.TernaryPointwiseOperation<Integer> operation = (background,
                                                                                   overlay,
                                                                                   paletted,
                                                                                   backgroundInBounds,
                                                                                   overlayInBounds,
                                                                                   palettedInBounds) -> {
                    paletted = stretcher.apply(paletted, palettedInBounds);

                    int resolvedPalette = paletteResolver.apply(paletted, palettedInBounds);
                    int[] toOverlay = includeBackground ? new int[]{overlay, resolvedPalette, background} : new int[]{overlay, resolvedPalette};
                    boolean[] toOverlayInBounds = includeBackground ? new boolean[]{overlayInBounds, palettedInBounds, backgroundInBounds} : new boolean[]{overlayInBounds, palettedInBounds};
                    return Operations.OVERLAY.apply(toOverlay, toOverlayInBounds);
                };

                return ImageUtils.generateScaledImage(operation, List.of(bImg, oImg, pImg));
            }
        };
    }
}
