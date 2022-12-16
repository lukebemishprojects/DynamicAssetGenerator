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
import dev.lukebemish.dynamicassetgenerator.impl.client.NativeImageHelper;
import dev.lukebemish.dynamicassetgenerator.impl.client.palette.ColorHolder;
import net.minecraft.server.packs.resources.IoSupplier;

import java.util.List;

public record ColorSource(List<Integer> color) implements ITexSource {
    public static final Codec<ColorSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.listOf().fieldOf("color").forGetter(s->s.color)
    ).apply(instance,ColorSource::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        return () -> {
            int len = Math.min(128*128,color.size());
            int sideLength = 0;
            for (int i = 0; i < 8; i++) {
                sideLength = (int) Math.pow(2,i);
                if (Math.pow(2,i)*Math.pow(2,i)>=len) {
                    break;
                }
            }
            NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA,sideLength,sideLength,false);
            outer:
            for (int y = 0; y < sideLength; y++) {
                for (int x = 0; x < sideLength; x++) {
                    if (x+sideLength*y >= len) {
                        break outer;
                    }
                    int hexColor = color.get(x+sideLength*y);
                    ColorHolder newColor = new ColorHolder(
                            (hexColor>>16&0xFF)/255f,
                            (hexColor>> 8&0xFF)/255f,
                            (hexColor    &0xFF)/255f,
                            (hexColor>>24&0xFF)/255f
                    );
                    out.setPixelRGBA(x,y, ColorHolder.toColorInt(newColor));
                }
            }
            return out;
        };
    }
}