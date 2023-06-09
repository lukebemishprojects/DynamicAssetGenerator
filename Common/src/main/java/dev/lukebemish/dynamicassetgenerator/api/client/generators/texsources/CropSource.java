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
import dev.lukebemish.dynamicassetgenerator.impl.client.NativeImageHelper;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

public final class CropSource implements TexSource {
    public static final Codec<CropSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("total_size").forGetter(CropSource::getTotalSize),
            Codec.INT.fieldOf("start_x").forGetter(CropSource::getStartX),
            Codec.INT.fieldOf("size_x").forGetter(CropSource::getSizeX),
            Codec.INT.fieldOf("start_y").forGetter(CropSource::getStartY),
            Codec.INT.fieldOf("size_y").forGetter(CropSource::getSizeY),
            TexSource.CODEC.fieldOf("input").forGetter(CropSource::getInput)
    ).apply(instance, CropSource::new));
    private final int totalSize;
    private final int startX;
    private final int sizeX;
    private final int startY;
    private final int sizeY;
    private final TexSource input;

    private CropSource(int totalSize, int startX, int sizeX, int startY, int sizeY, TexSource input) {
        this.totalSize = totalSize;
        this.startX = startX;
        this.sizeX = sizeX;
        this.startY = startY;
        this.sizeY = sizeY;
        this.input = input;
    }

    public Codec<CropSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> suppliedInput = getInput().getSupplier(data, context);
        if (suppliedInput == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", getInput().stringify());
            return null;
        }
        if (getSizeX() < 0 || getSizeY() < 0) {
            data.getLogger().error("Bounds of image are negative...\n{}", this.stringify());
            return null;
        }
        if (getTotalSize() <= 0) {
            data.getLogger().error("Total image width must be positive");
            return null;
        }
        return () -> {
            try (NativeImage inImg = suppliedInput.get()) {
                int scale = inImg.getWidth() / getTotalSize();

                if (scale == 0) {
                    data.getLogger().error("Image scale turned out to be 0! Image is {} wide, total width is {}",
                            inImg.getWidth(), getTotalSize());
                    throw new IOException("Image scale turned out to be 0! Image is " + inImg.getWidth() + " wide, total width is " + getTotalSize());
                }

                int distX = getSizeX() * scale;
                int distY = getSizeY() * scale;
                if (distY < 1 || distX < 1) {
                    data.getLogger().error("Bounds of image are non-positive! {}, {}", getSizeX(), getSizeY());
                    throw new IOException("Bounds of image are non-positive! " + getSizeX() + ", " + getSizeY());
                }

                NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA, distX, distY, false);
                for (int x = 0; x < distX; x++) {
                    for (int y = 0; y < distY; y++) {
                        int c = ImageUtils.safeGetPixelABGR(inImg, (x + getStartX() * scale), (y + getStartY() * scale));
                        out.setPixelRGBA(x, y, c);
                    }
                }
                return out;
            }
        };
    }

    public int getTotalSize() {
        return totalSize;
    }

    public int getStartX() {
        return startX;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getStartY() {
        return startY;
    }

    public int getSizeY() {
        return sizeY;
    }

    public TexSource getInput() {
        return input;
    }

    public static class Builder {
        private int totalSize;
        private int startX;
        private int sizeX;
        private int startY;
        private int sizeY;
        private TexSource input;

        public Builder setTotalSize(int totalSize) {
            this.totalSize = totalSize;
            return this;
        }

        public Builder setStartX(int startX) {
            this.startX = startX;
            return this;
        }

        public Builder setSizeX(int sizeX) {
            this.sizeX = sizeX;
            return this;
        }

        public Builder setStartY(int startY) {
            this.startY = startY;
            return this;
        }

        public Builder setSizeY(int sizeY) {
            this.sizeY = sizeY;
            return this;
        }

        public Builder setInput(TexSource input) {
            this.input = input;
            return this;
        }

        public CropSource build() {
            Objects.requireNonNull(input);
            return new CropSource(totalSize, startX, sizeX, startY, sizeY, input);
        }
    }
}
