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
import dev.lukebemish.dynamicassetgenerator.impl.client.util.SafeImageExtraction;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

public record Mask(ITexSource input, ITexSource mask) implements ITexSource {
    public static final Codec<Mask> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.CODEC.fieldOf("input").forGetter(Mask::input),
            ITexSource.CODEC.fieldOf("mask").forGetter(Mask::mask)
    ).apply(instance, Mask::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> input = this.input().getSupplier(data, context);
        IoSupplier<NativeImage> mask = this.mask().getSupplier(data, context);

        if (input == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.mask());
            return null;
        }
        if (mask == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.input());
            return null;
        }

        return () -> {
            try (NativeImage inImg = input.get();
                 NativeImage maskImg = mask.get()) {
                int maxX = Math.max(inImg.getWidth(), maskImg.getWidth());
                int maxY = inImg.getWidth() > maskImg.getWidth() ? inImg.getHeight() : maskImg.getHeight();
                int mxs, mys, ixs, iys;
                if (maskImg.getWidth() / (maskImg.getHeight() * 1.0) <= maxX / (maxY * 1.0)) {
                    mxs = maxX / maskImg.getWidth();
                    mys = maxY / maskImg.getWidth();
                } else {
                    mxs = maxX / maskImg.getHeight();
                    mys = maxY / maskImg.getHeight();
                }
                if (inImg.getWidth() / (inImg.getHeight() * 1.0) <= maxX / (maxY * 1.0)) {
                    ixs = inImg.getWidth() / maxX;
                    iys = inImg.getWidth() / maxY;
                } else {
                    ixs = inImg.getHeight() / maxX;
                    iys = inImg.getHeight() / maxY;
                }
                NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA, maxX, maxY, false);
                for (int x = 0; x < maxX; x++) {
                    for (int y = 0; y < maxY; y++) {
                        ColorHolder mC = ColorHolder.fromColorInt(SafeImageExtraction.get(maskImg, x / mxs, y / mys));
                        ColorHolder iC = ColorHolder.fromColorInt(SafeImageExtraction.get(inImg, x / ixs, y / iys));
                        ColorHolder o = iC.withA(mC.getA() * iC.getA());
                        out.setPixelRGBA(x, y, ColorHolder.toColorInt(o));
                    }
                }
                return out;
            }
        };
    }
}
