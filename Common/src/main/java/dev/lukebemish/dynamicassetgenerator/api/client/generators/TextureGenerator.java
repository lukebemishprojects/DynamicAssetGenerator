/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.IResourceGenerator;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Function;

public class TextureGenerator implements IResourceGenerator {
    public static final Codec<TextureGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("output_location").forGetter(dyn->dyn.outputLocation),
            ITexSource.CODEC.fieldOf("input").forGetter(dyn->dyn.input)
    ).apply(instance, TextureGenerator::new));

    private final ResourceLocation outputLocation;
    private final ITexSource input;

    private Function<ResourceGenerationContext, IoSupplier<NativeImage>> source;

    public TextureGenerator(ResourceLocation outputLocation, ITexSource source) {
        this.input = source;
        this.outputLocation = outputLocation;
        if (input!=null && outputLocation!=null) {
            this.source = context -> this.input.getSupplier(new TexSourceDataHolder(), context);
        } else {
            DynamicAssetGenerator.LOGGER.error("Could not set up DynamicTextureSource: {}", this);
        }
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
                DynamicAssetGenerator.LOGGER.error("Could not write image to stream: {}", outRl, e);
                throw e;
            } catch (JsonSyntaxException e) {
                DynamicAssetGenerator.LOGGER.error("Issue loading texture source JSON for output: {}", outRl, e);
                throw new IOException(e);
            } catch (Exception remainder) {
                DynamicAssetGenerator.LOGGER.error("Issue creating texture from source JSON for output: {}",outRl, remainder);
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
    public Codec<? extends IResourceGenerator> codec() {
        return CODEC;
    }
}
