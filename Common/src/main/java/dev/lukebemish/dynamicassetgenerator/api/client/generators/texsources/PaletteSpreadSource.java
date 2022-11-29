/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.NativeImageHelper;
import dev.lukebemish.dynamicassetgenerator.impl.client.palette.ColorHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.palette.Palette;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record PaletteSpreadSource(ITexSource source, float paletteCutoff, float lowerBound, float upperBound) implements ITexSource {
    public static final Codec<PaletteSpreadSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            ITexSource.CODEC.fieldOf("source").forGetter(PaletteSpreadSource::source),
            Codec.FLOAT.optionalFieldOf("palette_cutoff", Palette.DEFAULT_CUTOFF).forGetter(PaletteSpreadSource::paletteCutoff),
            Codec.FLOAT.optionalFieldOf("lower_bound",0f).forGetter(PaletteSpreadSource::lowerBound),
            Codec.FLOAT.optionalFieldOf("upper_bound",1f).forGetter(PaletteSpreadSource::upperBound)
    ).apply(i,PaletteSpreadSource::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @NotNull Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException {
        return () -> {
            Supplier<NativeImage> source = source().getSupplier(data);
            try (NativeImage sourceImg = source.get()) {
                if (sourceImg == null) {
                    data.getLogger().error("Texture given was nonexistent...\n{}", this.source());
                    return null;
                }
                Palette palette = Palette.extractPalette(sourceImg, 0, paletteCutoff());
                NativeImage outImg = NativeImageHelper.of(NativeImage.Format.RGBA, sourceImg.getWidth(), sourceImg.getHeight(), false);

                int maxIndex = palette.getSize()-1;
                float diff = upperBound()-lowerBound();

                for (int x = 0; x < sourceImg.getWidth(); x++) {
                    for (int y = 0; y < sourceImg.getHeight(); y++) {
                        ColorHolder original = ColorHolder.fromColorInt(sourceImg.getPixelRGBA(x,y));
                        if (original.getA() == 0) {
                            outImg.setPixelRGBA(x,y,0);
                            continue;
                        }
                        float ramp = ((float) palette.closestTo(original))/maxIndex;
                        float value = lowerBound()+(diff*ramp);
                        outImg.setPixelRGBA(x,y,ColorHolder.toColorInt(new ColorHolder(value,value,value,original.getA())));
                    }
                }

                return outImg;
            }
        };
    }
}
