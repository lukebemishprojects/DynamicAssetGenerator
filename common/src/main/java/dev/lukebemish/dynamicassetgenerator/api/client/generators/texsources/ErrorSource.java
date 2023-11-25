/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A {@link TexSource} that will always fail with a given message. Useful for debugging, or alongside
 * {@link FallbackSource} to provide more informative error messages.
 */
public final class ErrorSource implements TexSource {
    public static final Codec<ErrorSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("message").forGetter(ErrorSource::getMessage)
    ).apply(instance, ErrorSource::new));
    private final String message;

    private ErrorSource(String message) {
        this.message = message;
    }

    @Override
    public @NonNull Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        data.getLogger().error(getMessage());
        return null;
    }

    public String getMessage() {
        return message;
    }

    public static class Builder {
        private String message;

        /**
         * Sets the message to be logged when this source attempts and fails to provide a texture.
         */
        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public ErrorSource build() {
            Objects.requireNonNull(message);
            return new ErrorSource(message);
        }
    }
}
