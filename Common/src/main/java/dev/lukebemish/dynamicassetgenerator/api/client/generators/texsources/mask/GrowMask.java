/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A {@link TexSource} that grows the alpha channel of a source, and then applies a cutoff to
 * the result.
 */
public final class GrowMask implements TexSource {
    private static final float DEFAULT_GROWTH = 1f / 16f;
    private static final int DEFAULT_CUTOFF = 128;

    public static final Codec<GrowMask> CODEC = RecordCodecBuilder.create(i -> i.group(
            TexSource.CODEC.fieldOf("source").forGetter(GrowMask::getSource),
            Codec.FLOAT.optionalFieldOf("growth", DEFAULT_GROWTH).forGetter(GrowMask::getGrowth),
            Codec.INT.optionalFieldOf("cutoff", DEFAULT_CUTOFF).forGetter(GrowMask::getCutoff)
    ).apply(i, GrowMask::new));
    private final TexSource source;
    private final float growth;
    private final int cutoff;

    private GrowMask(TexSource source, float growth, int cutoff) {
        this.source = source;
        this.growth = growth;
        this.cutoff = cutoff;
    }

    @Override
    public @NotNull Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> input = this.source.getCachedSupplier(data, context);
        if (input == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.source.stringify());
            return null;
        }
        return () -> {
            try (NativeImage inImg = input.get()) {
                int width = inImg.getWidth();
                int height = inImg.getHeight();

                int toGrow = (int) Math.floor(width * growth);
                int filterSize = toGrow * toGrow * 2 + 1;
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
                    for (int y = 0; y < height; y++) {
                        boolean shouldGrow = false;
                        for (int i : xs) {
                            for (int j : ys) {
                                int x1 = x + i;
                                int y1 = y + j;
                                if (!(x1 < toGrow || y1 < toGrow || x1 >= width - toGrow || y1 >= width - toGrow) &&
                                        FastColor.ABGR32.alpha(inImg.getPixelRGBA(x1, y1)) >= cutoff)
                                    shouldGrow = true;
                            }
                        }

                        if (shouldGrow)
                            out.setPixelRGBA(x, y, 0xFFFFFFFF);
                        else
                            out.setPixelRGBA(x, y, 0);
                    }
                }
                return out;
            }
        };
    }

    public TexSource getSource() {
        return source;
    }

    public float getGrowth() {
        return growth;
    }

    public int getCutoff() {
        return cutoff;
    }


    public static class Builder {
        private TexSource source;
        private float growth = DEFAULT_GROWTH;
        private int cutoff = DEFAULT_CUTOFF;

        /**
         * Sets the input texture.
         */
        public Builder setSource(TexSource source) {
            this.source = source;
            return this;
        }

        /**
         *How much opaque regions should grow by, out of the image width.
         */
        public Builder setGrowth(float growth) {
            this.growth = growth;
            return this;
        }

        /**
         * The cutoff for which pixels are considered opaque.
         */
        public Builder setCutoff(int cutoff) {
            this.cutoff = cutoff;
            return this;
        }

        public GrowMask build() {
            Objects.requireNonNull(source);
            return new GrowMask(source, growth, cutoff);
        }
    }
}
