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
import dev.lukebemish.dynamicassetgenerator.impl.util.MultiCloser;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record Overlay(List<ITexSource> inputs) implements ITexSource {
    public static final Codec<Overlay> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.CODEC.listOf().fieldOf("inputs").forGetter(Overlay::inputs)
    ).apply(instance, Overlay::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        List<IoSupplier<NativeImage>> inputs = new ArrayList<>();
        for (ITexSource o : this.inputs()) {
            inputs.add(o.getSupplier(data, context));
        }
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i)==null) {
                data.getLogger().error("Texture given was nonexistent...\n{}",this.inputs().get(i).toString());
                return null;
            }
        }
        return () -> {
            int maxWidth = 0;
            int maxHeight = 0;
            List<NativeImage> images = new ArrayList<>();
            for (var input : inputs) {
                images.add(input.get());
            }
            for (NativeImage image : images) {
                if (image.getWidth() > maxWidth) {
                    maxWidth = image.getWidth();
                }
            }
            for (NativeImage image : images) {
                int modifiedHeight = image.getHeight() * (maxWidth / image.getWidth());
                if (modifiedHeight > maxHeight) {
                    maxHeight = modifiedHeight;
                }
            }
            try (MultiCloser ignored = new MultiCloser(images)) {
                NativeImage output = NativeImageHelper.of(NativeImage.Format.RGBA, maxWidth, maxHeight, false);
                NativeImage base = images.get(0);
                int scale = maxWidth / base.getWidth();
                for (int x = 0; x < maxWidth; x++) {
                    for (int y = 0; y < maxHeight; y++) {
                        output.setPixelRGBA(x, y, SafeImageExtraction.get(base, x / scale, y / scale));
                    }
                }
                if (images.size() >= 2) {
                    for (int i = 1; i < images.size(); i++) {
                        NativeImage image = images.get(i);
                        scale = maxWidth / base.getWidth();
                        for (int x = 0; x < maxWidth; x++) {
                            for (int y = 0; y < maxHeight; y++) {
                                ColorHolder input = ColorHolder.fromColorInt(SafeImageExtraction.get(output, x, y));
                                ColorHolder top = ColorHolder.fromColorInt(SafeImageExtraction.get(image, x / scale, y / scale));
                                ColorHolder outColor = ColorHolder.alphaBlend(top, input);
                                output.setPixelRGBA(x, y, ColorHolder.toColorInt(outColor));
                            }
                        }
                    }
                }
                return output;
            }
        };
    }
}
