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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * A resource generator that generates a PNG texture as specified by a {@link TexSource}. As {@link TexSource}s are
 * cached in memory to avoid regenerating duplicate parts of a texture, this generator should only be used with a
 * {@link dev.lukebemish.dynamicassetgenerator.api.client.AssetResourceCache}.
 */
public class TextureGenerator implements ResourceGenerator {
    public static final Codec<TextureGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("output_location").forGetter(dyn->dyn.outputLocation),
            TexSource.CODEC.fieldOf("input").forGetter(dyn->dyn.input)
    ).apply(instance, TextureGenerator::new));

    private final ResourceLocation outputLocation;
    private final TexSource input;

    /**
     * @param outputLocation the location to generate a texture at, excluding the "textures/" prefix or ".png" extension
     * @param source the texture source to generate
     */
    public TextureGenerator(@NotNull ResourceLocation outputLocation, @NotNull TexSource source) {
        this.input = source;
        this.outputLocation = outputLocation;
    }

    @Override
    public IoSupplier<InputStream> get(ResourceLocation outRl, ResourceGenerationContext context) {
        IoSupplier<NativeImage> imageGetter = this.input.getSupplier(new TexSourceDataHolder(), context);
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

    /**
     * This method should be considered internal, but to avoid breaking backwards compatibility, no breaking changes
     * will be made until DynAssetGen 5.0.0 or later.
     */
    @ApiStatus.Internal
    public ResourceLocation getOutputLocation() {
        return new ResourceLocation(this.outputLocation.getNamespace(), "textures/"+this.outputLocation.getPath()+".png");
    }

    @Override
    public Codec<? extends ResourceGenerator> codec() {
        return CODEC;
    }
}
