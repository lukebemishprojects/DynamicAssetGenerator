/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.io.IOException;

public record FallbackSource(ITexSource original, ITexSource fallback) implements ITexSource {
    public static final Codec<FallbackSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.CODEC.fieldOf("original").forGetter(FallbackSource::original),
            ITexSource.CODEC.fieldOf("fallback").forGetter(FallbackSource::fallback)
    ).apply(instance, FallbackSource::new));

    public Codec<FallbackSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) throws JsonSyntaxException{
        TexSourceDataHolder newData = new TexSourceDataHolder(data);
        newData.put(Logger.class, NOPLogger.NOP_LOGGER);
        IoSupplier<NativeImage> original = this.original().getSupplier(newData, context);
        IoSupplier<NativeImage> fallback = this.fallback().getSupplier(data, context);

        if (original==null && fallback==null) {
            data.getLogger().error("Both textures given were nonexistent...");
            return null;
        }

        return () -> {
            if (original != null) {
                try {
                    return original.get();
                } catch (IOException ignored) {}
            }
            if (fallback != null)
                return fallback.get();
            data.getLogger().error("Both textures given were unloadable...");
            throw new IOException("Both textures given were unloadable...");
        };
    }
}
