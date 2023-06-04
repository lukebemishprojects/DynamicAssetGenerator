/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Function;

public class TextureGenerator implements ResourceGenerator {
    public static final Codec<TextureGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("output_location").forGetter(dyn->dyn.outputLocation),
            TexSource.CODEC.fieldOf("input").forGetter(dyn->dyn.input)
    ).apply(instance, TextureGenerator::new));

    private final ResourceLocation outputLocation;
    private final TexSource input;

    private Function<ResourceGenerationContext, IoSupplier<NativeImage>> source;

    public TextureGenerator(@NotNull ResourceLocation outputLocation, @NotNull TexSource source) {
        this.input = source;
        this.outputLocation = outputLocation;
        this.source = context -> this.input.getSupplier(new TexSourceDataHolder(), context);
    }

    @Override
    public IoSupplier<InputStream> get(ResourceLocation outRl, ResourceGenerationContext context) {
        if (this.source == null) return null;
        IoSupplier<NativeImage> imageGetter = this.source.apply(context);
        if (imageGetter == null) return null;
        return () -> {
            try (NativeImage image = imageGetter.get()) {
                return new ByteArrayInputStream(image.asByteArray());
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Could not write image to stream for source {}: {}", input.stringify(), outRl, e);
                throw e;
            } catch (Exception remainder) {
                DynamicAssetGenerator.LOGGER.error("Unknown issue creating texture for output {} with source {}", outRl, input.stringify(), remainder);
                throw new IOException(remainder);
            }
        };
    }

    @Override
    public @NotNull Set<ResourceLocation> getLocations() {
        return Set.of(getOutputLocation());
    }

    public ResourceLocation getOutputLocation() {
        return new ResourceLocation(this.outputLocation.getNamespace(), "textures/"+this.outputLocation.getPath()+".png");
    }

    @Override
    public Codec<? extends ResourceGenerator> codec() {
        return CODEC;
    }
}
