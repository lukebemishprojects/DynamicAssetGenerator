/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.ClientPrePackRepository;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.util.Objects;

public final class TextureReaderSource implements TexSource {
    public static final Codec<TextureReaderSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("path").forGetter(TextureReaderSource::getPath)
    ).apply(instance, TextureReaderSource::new));
    private final ResourceLocation path;

    private TextureReaderSource(ResourceLocation path) {
        this.path = path;
    }

    @Override
    public Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        ResourceLocation outRl = new ResourceLocation(this.getPath().getNamespace(), "textures/" + this.getPath().getPath() + ".png");
        return () -> {
            try {
                return NativeImage.read(ClientPrePackRepository.getResource(outRl));
            } catch (IOException e) {
                data.getLogger().error("Issue loading texture: {}", this.getPath());
            }
            throw new IOException("Issue loading texture: " + this.getPath());
        };
    }

    public ResourceLocation getPath() {
        return path;
    }

    public static class Builder {
        private ResourceLocation path;

        public Builder setPath(ResourceLocation path) {
            this.path = path;
            return this;
        }

        public TextureReaderSource build() {
            Objects.requireNonNull(path);
            return new TextureReaderSource(path);
        }
    }
}
