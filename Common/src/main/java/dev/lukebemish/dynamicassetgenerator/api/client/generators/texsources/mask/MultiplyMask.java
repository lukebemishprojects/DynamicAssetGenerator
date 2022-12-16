/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.mask;

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

public record MultiplyMask(List<ITexSource> sources) implements ITexSource {
    public static final Codec<MultiplyMask> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.CODEC.listOf().fieldOf("sources").forGetter(MultiplyMask::sources)
    ).apply(instance, MultiplyMask::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        List<IoSupplier<NativeImage>> inputs = new ArrayList<>();
        for (ITexSource o : this.sources()) {
            inputs.add(o.getSupplier(data, context));
        }
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i)==null) {
                data.getLogger().error("Texture given was nonexistent...\n{}",this.sources().get(i).toString());
                return null;
            }
        }
        return () -> {
            int maxX = 0;
            int maxY = 0;
            List<NativeImage> images = new ArrayList<>();
            for (var input : inputs) {
                images.add(input.get());
            }
            for (NativeImage image : images) {
                if (image.getWidth() > maxX) {
                    maxX = image.getWidth();
                    maxY = image.getHeight();
                }
            }
            try (MultiCloser ignored = new MultiCloser(images)) {
                NativeImage output = NativeImageHelper.of(NativeImage.Format.RGBA, maxX, maxY, false);
                NativeImage base = images.get(0);
                int xs;
                int ys;
                if (base.getWidth() / (base.getHeight() * 1.0) <= maxX / (maxY * 1.0)) {
                    xs = maxX / base.getWidth();
                    ys = maxY / base.getWidth();
                } else {
                    xs = maxX / base.getHeight();
                    ys = maxY / base.getHeight();
                }
                for (int x = 0; x < maxX; x++) {
                    for (int y = 0; y < maxY; y++) {
                        output.setPixelRGBA(x, y, SafeImageExtraction.get(base, x / xs, y / ys));
                    }
                }

                for (int x = 0; x < maxX; x++) {
                    for (int y = 0; y < maxY; y++) {
                        ColorHolder colorHolder = SafeImageExtraction.getColor(base, x / xs, y / ys);
                        if (images.size() >= 2) {
                            for (int i = 1; i < images.size(); i++) {
                                NativeImage image = images.get(i);
                                if (image.getWidth() / (image.getHeight() * 1.0) <= maxX / (maxY * 1.0)) {
                                    xs = maxX / image.getWidth();
                                    ys = maxY / image.getWidth();
                                } else {
                                    xs = maxX / image.getHeight();
                                    ys = maxY / image.getHeight();
                                }
                                ColorHolder top = ColorHolder.fromColorInt(SafeImageExtraction.get(image, x / xs, y / ys));
                                colorHolder = new ColorHolder(
                                        colorHolder.getR()*top.getR(),
                                        colorHolder.getG()*top.getG(),
                                        colorHolder.getB()*top.getB(),
                                        colorHolder.getA()*top.getA());
                            }
                        }
                        output.setPixelRGBA(x,y,colorHolder.toInt());
                    }
                }
                return output;
            }
        };
    }
}
