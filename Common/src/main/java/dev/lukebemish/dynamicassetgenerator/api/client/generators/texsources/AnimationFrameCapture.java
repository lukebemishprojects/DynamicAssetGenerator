/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record AnimationFrameCapture(String capture) implements ITexSource {
    public static final Codec<AnimationFrameCapture> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("capture").forGetter(AnimationFrameCapture::capture)
    ).apply(instance, AnimationFrameCapture::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @NotNull Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException {
        return () -> {
            AnimationSplittingSource.ImageCollection collection = data.get(AnimationSplittingSource.ImageCollection.class);
            if (collection==null) {
                data.getLogger().debug("No parent animation source to capture...");
                return null;
            }
            NativeImage image = collection.get(this.capture());
            if (image==null) {
                data.getLogger().debug("Key '{}' was not supplied to capture...",capture());
            }
            return image;
        };
    }
}
