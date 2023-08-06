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
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A {@link TexSource} that inverts the color of a source.
 */
public final class InvertMask implements TexSource {
    public static final Codec<InvertMask> CODEC = RecordCodecBuilder.create(i -> i.group(
            TexSource.CODEC.fieldOf("source").forGetter(InvertMask::getSource)
    ).apply(i, InvertMask::new));
    private final TexSource source;

    private InvertMask(TexSource source) {
        this.source = source;
    }

    @Override
    public Codec<? extends TexSource> codec() {
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
                NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA, width, height, false);
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < width; y++) {
                        int source = inImg.getPixelRGBA(x, y);
                        out.setPixelRGBA(x, y, ~source);
                    }
                }
                return out;
            }
        };
    }

    public TexSource getSource() {
        return source;
    }

    public static class Builder {
        private TexSource source;

        /**
         * Sets the input texture.
         */
        public Builder setSource(TexSource source) {
            this.source = source;
            return this;
        }

        public InvertMask build() {
            Objects.requireNonNull(source);
            return new InvertMask(source);
        }
    }
}
