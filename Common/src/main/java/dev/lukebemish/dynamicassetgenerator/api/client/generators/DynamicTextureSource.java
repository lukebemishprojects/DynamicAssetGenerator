/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.api.IResourceGenerator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Supplier;

public class DynamicTextureSource implements IResourceGenerator {
    public static final Codec<DynamicTextureSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("output_location").forGetter(dyn->dyn.outputLocation),
            ITexSource.CODEC.fieldOf("input").forGetter(dyn->dyn.input)
    ).apply(instance, DynamicTextureSource::new));

    private final ResourceLocation outputLocation;
    private final ITexSource input;

    private Supplier<NativeImage> source;

    public DynamicTextureSource(ResourceLocation outputLocation, ITexSource source) {
        this.input = source;
        this.outputLocation = outputLocation;
        if (input!=null && outputLocation!=null) {
            this.source = () -> this.input.getSupplier(new TexSourceDataHolder()).get();
        } else {
            DynamicAssetGenerator.LOGGER.error("Could not set up DynamicTextureSource: {}", this);
        }
    }

    @Override
    public @NotNull Supplier<InputStream> get(ResourceLocation outRl) {
        if (this.source == null) return ()->null;
        return () -> {
            try (NativeImage image = source.get()) {
                if (image != null) {
                    return new ByteArrayInputStream(image.asByteArray());
                }
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Could not write image to stream: {}", outRl, e);
            } catch (JsonSyntaxException e) {
                DynamicAssetGenerator.LOGGER.error("Issue loading texture source JSON for output: {}", outRl, e);
            } catch (Exception remainder) {
                DynamicAssetGenerator.LOGGER.error("Issue creating texture from source JSON for output: {}",outRl, remainder);
            }
            return null;
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
