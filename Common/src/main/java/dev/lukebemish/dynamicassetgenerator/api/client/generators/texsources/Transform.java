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
import dev.lukebemish.dynamicassetgenerator.api.client.image.ImageUtils;
import dev.lukebemish.dynamicassetgenerator.impl.client.NativeImageHelper;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class Transform implements TexSource {
    public static final Codec<Transform> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TexSource.CODEC.fieldOf("input").forGetter(Transform::getInput),
            Codec.INT.fieldOf("rotate").forGetter(Transform::getRotate),
            Codec.BOOL.fieldOf("flip").forGetter(Transform::isFlip)
    ).apply(instance, Transform::new));
    private final TexSource input;
    private final int rotate;
    private final boolean flip;

    private Transform(TexSource input, int rotate, boolean flip) {
        this.input = input;
        this.rotate = rotate;
        this.flip = flip;
    }

    @Override
    public Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> input = this.getInput().getSupplier(data, context);
        if (input == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.getInput());
            return null;
        }
        return () -> {
            NativeImage output = input.get();
            for (int i = 0; i < this.getRotate(); i++) {
                output = clockwiseRotate(output);
            }
            if (this.isFlip()) {
                NativeImage output2 = NativeImageHelper.of(output.format(), output.getWidth(), output.getHeight(), false);
                for (int x = 0; x < output.getWidth(); x++) {
                    for (int y = 0; y < output.getHeight(); y++) {
                        output2.setPixelRGBA((output.getWidth() - 1 - x), y, ImageUtils.safeGetPixelABGR(output, x, y));
                    }
                }
                output.close();
                output = output2;
            }
            return output;
        };
    }

    private static NativeImage clockwiseRotate(NativeImage input) {
        int w = input.getWidth();
        int h = input.getHeight();
        NativeImage output = NativeImageHelper.of(input.format(), h, w, false);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                //noinspection SuspiciousNameCombination
                output.setPixelRGBA(y, w - x - 1, ImageUtils.safeGetPixelABGR(input, x, y));
        input.close();
        return output;
    }

    public TexSource getInput() {
        return input;
    }

    public int getRotate() {
        return rotate;
    }

    public boolean isFlip() {
        return flip;
    }

    public static class Builder {
        private TexSource input;
        private int rotate;
        private boolean flip;

        public Builder setInput(TexSource input) {
            this.input = input;
            return this;
        }

        public Builder setRotate(int rotate) {
            this.rotate = rotate;
            return this;
        }

        public Builder setFlip(boolean flip) {
            this.flip = flip;
            return this;
        }

        public Transform build() {
            Objects.requireNonNull(input);
            return new Transform(input, rotate, flip);
        }
    }
}
