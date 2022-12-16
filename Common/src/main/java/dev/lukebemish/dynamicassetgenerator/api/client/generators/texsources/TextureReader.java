/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.ClientPrePackRepository;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;

public record TextureReader(ResourceLocation path) implements ITexSource {
    public static final Codec<TextureReader> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("path").forGetter(TextureReader::path)
    ).apply(instance, TextureReader::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        ResourceLocation outRl = new ResourceLocation(this.path().getNamespace(), "textures/"+this.path().getPath()+".png");
        return () -> {
            try {
                return NativeImage.read(ClientPrePackRepository.getResource(outRl));
            } catch (IOException e) {
                data.getLogger().error("Issue loading texture: {}", this.path());
            }
            throw new IOException("Issue loading texture: "+this.path());
        };
    }
}
