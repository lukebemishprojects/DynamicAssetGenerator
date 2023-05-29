/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public record AnimationFrameCapture(String capture) implements TexSource {
    public static final Codec<AnimationFrameCapture> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("capture").forGetter(AnimationFrameCapture::capture)
    ).apply(instance, AnimationFrameCapture::new));

    @Override
    public Codec<? extends TexSource> codec() {
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

    @Override
    @NotNull
    public <T> DataResult<T> cacheMetadata(DynamicOps<T> ops, TexSourceDataHolder data) {
        AnimationSplittingSource.ImageCollection collection = data.get(AnimationSplittingSource.ImageCollection.class);
        if (collection != null) {
            var builder = ops.mapBuilder();
            builder.add("frame",ops.createInt(collection.getFrame()));
            AnimationSplittingSource.TimeAwareSource source = collection.getFull(capture());
            if (source == null)
                return DataResult.error(() -> "In uncacheable state, no parent animation source to capture...");
            DataResult<T> parentElementTyped = TexSource.CODEC.encodeStart(ops, source.source());
            builder.add("scale", ops.createInt(source.scale()));
            if (parentElementTyped.result().isEmpty())
                return DataResult.error(() -> "Could not encode parent animation source: "+ parentElementTyped.error().get().message());
            builder.add("parent",parentElementTyped);
            return builder.build(ops.empty());
        }
        return DataResult.error(() -> "In uncacheable state, no parent animation source to capture...");
    }
}
