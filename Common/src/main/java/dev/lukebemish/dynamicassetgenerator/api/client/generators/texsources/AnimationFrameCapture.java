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
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;

public record AnimationFrameCapture(String capture) implements ITexSource {
    public static final Codec<AnimationFrameCapture> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("capture").forGetter(AnimationFrameCapture::capture)
    ).apply(instance, AnimationFrameCapture::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        return () -> {
            AnimationSplittingSource.ImageCollection collection = data.get(AnimationSplittingSource.ImageCollection.class);
            if (collection==null) {
                data.getLogger().debug("No parent animation source to capture...");
                throw new IOException("No parent animation source to capture...");
            }
            NativeImage image = collection.get(this.capture());
            if (image==null) {
                data.getLogger().debug("Key '{}' was not supplied to capture...",capture());
                throw new IOException("Key '"+capture()+"' was not supplied to capture...");
            }
            return image;
        };
    }
}
