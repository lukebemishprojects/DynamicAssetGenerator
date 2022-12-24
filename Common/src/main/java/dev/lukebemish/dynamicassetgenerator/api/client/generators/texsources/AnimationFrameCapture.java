/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;

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

    @Override
    @NotNull
    public <T> DataResult<T> cacheMetadata(DynamicOps<T> ops, TexSourceDataHolder data) {
        AnimationSplittingSource.ImageCollection collection = data.get(AnimationSplittingSource.ImageCollection.class);
        if (collection != null) {
            var builder = ops.mapBuilder();
            builder.add("frame",ops.createInt(collection.getFrame()));
            AnimationSplittingSource.TimeAwareSource source = collection.getFull(capture());
            DataResult<JsonElement> parentElement = ITexSource.CODEC.encodeStart(JsonOps.INSTANCE, source.source());
            builder.add("scale", ops.createInt(source.scale()));
            if (parentElement.result().isEmpty())
                return DataResult.error("Could not encode parent animation source: "+ parentElement.error().get().message());
            builder.add("parent",JsonOps.INSTANCE.convertTo(ops, parentElement.result().get()));
            return builder.build(ops.empty());
        }
        return DataResult.error("No image collection found in data holder; cannot cache");
    }
}
