/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.NativeImageHelper;
import dev.lukebemish.dynamicassetgenerator.impl.client.util.SafeImageExtraction;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

public record Transform(ITexSource input, int rotate, boolean flip) implements ITexSource {
    public static final Codec<Transform> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.CODEC.fieldOf("input").forGetter(Transform::input),
            Codec.INT.fieldOf("rotate").forGetter(Transform::rotate),
            Codec.BOOL.fieldOf("flip").forGetter(Transform::flip)
    ).apply(instance, Transform::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data) {
        IoSupplier<NativeImage> input = this.input().getSupplier(data);
        if (input == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.input());
            return null;
        }
        return () -> {
            NativeImage output = input.get();
            for (int i = 0; i < this.rotate(); i++) {
                output = clockwiseRotate(output);
            }
            if (this.flip()) {
                NativeImage output2 = NativeImageHelper.of(output.format(), output.getWidth(), output.getHeight(), false);
                for (int x = 0; x < output.getWidth(); x++) {
                    for (int y = 0; y < output.getHeight(); y++) {
                        output2.setPixelRGBA((output.getWidth()-1-x),y, SafeImageExtraction.get(output,x,y));
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
                output.setPixelRGBA(y, w - x - 1, SafeImageExtraction.get(input,x, y));
        input.close();
        return output;
    }
}
