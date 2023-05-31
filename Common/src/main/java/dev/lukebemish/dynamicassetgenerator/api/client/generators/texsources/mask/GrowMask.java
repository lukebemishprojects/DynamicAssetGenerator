/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.mask;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.NativeImageHelper;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

public record GrowMask(TexSource source, float growth, int cutoff) implements TexSource {
    public static final Codec<GrowMask> CODEC = RecordCodecBuilder.create(i -> i.group(
            TexSource.CODEC.fieldOf("source").forGetter(GrowMask::source),
            Codec.FLOAT.optionalFieldOf("growth",1f/16f).forGetter(GrowMask::growth),
            Codec.INT.optionalFieldOf("cutoff",128).forGetter(GrowMask::cutoff)
    ).apply(i, GrowMask::new));

    @Override
    public Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> input = this.source.getSupplier(data, context);
        if (input == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.source);
            return null;
        }
        return () -> {
            try (NativeImage inImg = input.get()) {
                int width = inImg.getWidth();
                int height = inImg.getHeight();

                int toGrow = (int) Math.floor(width * growth);
                int filterSize = toGrow*toGrow*2+1;
                int[] xs = new int[filterSize];
                int[] ys = new int[filterSize];
                int counter = 0;
                for (int x = -toGrow; x <= toGrow; x++) {
                    for (int y = -toGrow; y <= toGrow; y++) {
                        xs[counter] = x;
                        ys[counter] = y;
                        counter++;
                    }
                }

                NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA, width, height, false);
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < width; y++) {
                        boolean shouldGrow = false;
                        for (int i : xs) {
                            for (int j : ys) {
                                int x1 = x+i;
                                int y1 = y+j;
                                if (!(x1 < toGrow || y1 < toGrow || x1 >= width - toGrow || y1 >= width - toGrow) &&
                                        FastColor.ABGR32.alpha(inImg.getPixelRGBA(x1,y1)) >= cutoff)
                                    shouldGrow = true;
                            }
                        }

                        if (shouldGrow)
                            out.setPixelRGBA(x,y,0xFFFFFFFF);
                        else
                            out.setPixelRGBA(x,y,0);
                    }
                }
                return out;
            }
        };
    }
}
