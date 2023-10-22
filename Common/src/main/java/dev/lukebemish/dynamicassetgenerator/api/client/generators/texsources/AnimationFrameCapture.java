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
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.Objects;

/**
 * A {@link TexSource} that captures a frame from a source split up by an {@link AnimationSplittingSource}.
 */
public final class AnimationFrameCapture implements TexSource {
    public static final Codec<AnimationFrameCapture> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("capture").forGetter(AnimationFrameCapture::getCapture)
    ).apply(instance, AnimationFrameCapture::new));
    private final String capture;

    private AnimationFrameCapture(String capture) {
        this.capture = capture;
    }

    @Override
    public @NonNull Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        return () -> {
            AnimationSplittingSource.ImageCollection collection = data.get(AnimationSplittingSource.IMAGE_COLLECTION_TOKEN);
            if (collection == null) {
                data.getLogger().debug("No parent animation source to capture...");
                throw new IOException("No parent animation source to capture...");
            }
            NativeImage image = collection.get(this.getCapture());
            if (image == null) {
                data.getLogger().debug("Key '{}' was not supplied to capture...", getCapture());
                throw new IOException("Key '" + getCapture() + "' was not supplied to capture...");
            }
            return image;
        };
    }

    @Override
    @NonNull
    public <T> DataResult<T> cacheMetadata(DynamicOps<T> ops, TexSourceDataHolder data) {
        AnimationSplittingSource.ImageCollection collection = data.get(AnimationSplittingSource.IMAGE_COLLECTION_TOKEN);
        if (collection != null) {
            var builder = ops.mapBuilder();
            builder.add("frame", ops.createInt(collection.getFrame()));
            TexSource source = collection.getFull(getCapture());
            if (source == null)
                return DataResult.error(() -> "In uncacheable state, no parent animation source to capture...");
            DataResult<T> parentElementTyped = TexSource.CODEC.encodeStart(ops, source);
            if (parentElementTyped.error().isPresent())
                //noinspection OptionalGetWithoutIsPresent
                return DataResult.error(() -> "Could not encode parent animation source: " + parentElementTyped.error().get().message());
            builder.add("parent", parentElementTyped);
            return builder.build(ops.empty());
        }
        // we're looking from the outside in, so there is no metadata to cache or this is an error state anyway.
        return TexSource.super.cacheMetadata(ops, data);
    }

    public String getCapture() {
        return capture;
    }

    public static class Builder {
        private String capture;

        /**
         * Sets the key of the source to capture a frame of.
         */
        public Builder setCapture(String capture) {
            this.capture = capture;
            return this;
        }

        public AnimationFrameCapture build() {
            Objects.requireNonNull(capture);
            return new AnimationFrameCapture(capture);
        }
    }
}
