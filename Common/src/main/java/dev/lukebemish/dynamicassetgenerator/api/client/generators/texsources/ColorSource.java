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
import dev.lukebemish.dynamicassetgenerator.api.colors.ColorEncoding;
import dev.lukebemish.dynamicassetgenerator.impl.client.NativeImageHelper;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Objects;

public final class ColorSource implements TexSource {
    public static final ColorEncoding DEFAULT_COLOR_ENCODING = ColorEncoding.ARGB;

    public static final Codec<ColorSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.listOf().fieldOf("color").forGetter(s -> s.color),
            StringRepresentable.fromEnum(ColorEncoding::values).optionalFieldOf("encoding", DEFAULT_COLOR_ENCODING).forGetter(ColorSource::getColorEncoding)
    ).apply(instance, ColorSource::new));
    private final List<Integer> color;
    private final ColorEncoding colorEncoding;

    private ColorSource(List<Integer> color, ColorEncoding colorEncoding) {
        this.color = color;
        this.colorEncoding = colorEncoding;
    }

    @Override
    public Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        return () -> {
            int len = color.size();
            int sideLength = 0;
            for (int i = 0; i < 8; i++) {
                sideLength = (int) Math.pow(2, i);
                if (Math.pow(2, i) * Math.pow(2, i) >= len) {
                    break;
                }
            }
            NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA, sideLength, sideLength, false);
            outer:
            for (int y = 0; y < sideLength; y++) {
                for (int x = 0; x < sideLength; x++) {
                    if (x + sideLength * y >= len) {
                        break outer;
                    }
                    int encodedColor = colorEncoding.toABGR.applyAsInt(color.get(x + sideLength * y));
                    out.setPixelRGBA(x, y, encodedColor);
                }
            }
            return out;
        };
    }

    public List<Integer> getColor() {
        return color;
    }

    public ColorEncoding getColorEncoding() {
        return colorEncoding;
    }

    public static class Builder {
        private List<Integer> color;
        private ColorEncoding colorEncoding = DEFAULT_COLOR_ENCODING;

        public Builder setColor(List<Integer> color) {
            this.color = color;
            return this;
        }

        public Builder setColorEncoding(ColorEncoding colorEncoding) {
            this.colorEncoding = colorEncoding;
            return this;
        }

        public ColorSource build() {
            Objects.requireNonNull(color);
            return new ColorSource(color, colorEncoding);
        }
    }
}