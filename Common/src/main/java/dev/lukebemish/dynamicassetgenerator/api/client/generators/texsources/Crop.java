/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import java.io.IOException;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.NativeImageHelper;
import dev.lukebemish.dynamicassetgenerator.impl.client.util.SafeImageExtraction;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.server.packs.resources.IoSupplier;

public record Crop(int totalSize, int startX, int sizeX, int startY, int sizeY, ITexSource input) implements ITexSource {
    public static final Codec<Crop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("total_size").forGetter(Crop::totalSize),
            Codec.INT.fieldOf("start_x").forGetter(Crop::startX),
            Codec.INT.fieldOf("size_x").forGetter(Crop::sizeX),
            Codec.INT.fieldOf("start_y").forGetter(Crop::startY),
            Codec.INT.fieldOf("size_y").forGetter(Crop::sizeY),
            ITexSource.CODEC.fieldOf("input").forGetter(Crop::input)
    ).apply(instance, Crop::new));

    public Codec<Crop> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> suppliedInput = input().getSupplier(data, context);
        if (suppliedInput == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", input());
            return null;
        }
        if (sizeX() < 0 || sizeY() < 0) {
            data.getLogger().error("Bounds of image are negative...\n{}", this);
            return null;
        }
        return () -> {
            try (NativeImage inImg = suppliedInput.get()) {
                if (totalSize() == 0) {
                    data.getLogger().error("Total image width must be non-zero");
                    throw new IOException("Total image width must be non-zero");
                }
                int scale = inImg.getWidth() / totalSize();

                if (scale == 0) {
                    data.getLogger().error("Image scale turned out to be 0! Image is {} wide, total width is {}",
                            inImg.getWidth(), totalSize());
                    throw new IOException("Image scale turned out to be 0! Image is " + inImg.getWidth() + " wide, total width is " + totalSize());
                }

                int distX = sizeX() * scale;
                int distY = sizeY() * scale;
                if (distY < 1 || distX < 1) {
                    data.getLogger().error("Bounds of image are negative! {}, {}", sizeX(), sizeY());
                    throw new IOException("Bounds of image are negative! "+sizeX()+", "+sizeY());
                }

                NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA, distX, distY, false);
                for (int x = 0; x < distX; x++) {
                    for (int y = 0; y < distY; y++) {
                        int c = SafeImageExtraction.get(inImg, (x + startX() * scale), (y + startY() * scale));
                        out.setPixelRGBA(x, y, c);
                    }
                }
                return out;
            }
        };
    }
}
