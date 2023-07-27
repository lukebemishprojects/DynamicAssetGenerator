/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TextureMetaGenerator;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TouchedTextureTracker;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.mixin.SpriteSourcesAccessor;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;

public record TexSourceSpriteSource(Map<ResourceLocation, TexSource> sources, ResourceLocation location) implements SpriteSource {
    public static final ResourceLocation LOCATION = new ResourceLocation(DynamicAssetGenerator.MOD_ID, "tex_sources");
    public static Codec<TexSourceSpriteSource> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.unboundedMap(ResourceLocation.CODEC, TexSource.CODEC).fieldOf("sources").forGetter(TexSourceSpriteSource::sources),
        ResourceLocation.CODEC.fieldOf("location").forGetter(TexSourceSpriteSource::location)
    ).apply(i, TexSourceSpriteSource::new));

    @Override
    public void run(ResourceManager resourceManager, Output output) {
        ResourceGenerationContext context = new ResourceGenerationContext() {
            @Override
            public @NotNull ResourceLocation getCacheName() {
                return location();
            }

            @Override
            public @Nullable IoSupplier<InputStream> getResource(@NotNull ResourceLocation location) {
                return resourceManager.getResource(location).<IoSupplier<InputStream>>map(resource -> resource::open).orElse(null);
            }

            @Override
            public void listResources(@NotNull String namespace, @NotNull String path, PackResources.@NotNull ResourceOutput resourceOutput) {
                resourceManager.listResourceStacks(path, rl -> rl.getNamespace().equals(namespace)).forEach((rl, resources) ->
                    resources.forEach(resource -> resourceOutput.accept(rl, resource::open)));
            }

            @Override
            public @NotNull Set<String> getNamespaces() {
                return resourceManager.getNamespaces();
            }
        };

        resourceManager.listResources(location.getNamespace()+"/"+location.getPath(), rl -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
            try (var reader = resource.openAsReader()) {
                JsonElement json = DynamicAssetGenerator.GSON.fromJson(reader, JsonElement.class);
                var result = TexSource.CODEC.parse(JsonOps.INSTANCE, json);
                result.result().ifPresent(texSource -> {
                    ResourceLocation sourceLocation = new ResourceLocation(rl.getNamespace(), rl.getPath().substring(0, rl.getPath().length()-5));
                    sources.put(sourceLocation, texSource);
                });
                result.error().ifPresent(partial ->
                    DynamicAssetGenerator.LOGGER.error("Failed to load tex source json for "+ location +": "+rl+": "+partial.message()));
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Failed to load tex source json for "+ location +": "+rl, e);
            }
        });

        sources.forEach((rl, texSource) -> {
            var dataHolder = new TexSourceDataHolder();
            dataHolder.put(TouchedTextureTracker.class, new TouchedTextureTracker());
            var imageSupplier = texSource.getSupplier(dataHolder, context);
            output.add(rl, () -> {
                try {
                    if (imageSupplier == null) {
                        throw new IOException("No image supplier");
                    }
                    NativeImage image = imageSupplier.get();
                    TouchedTextureTracker tracker = dataHolder.get(TouchedTextureTracker.class);
                    AnimationMetadataSection section = AnimationMetadataSection.EMPTY;
                    if (tracker != null && tracker.getTouchedTextures().size() >= 1) {
                        TextureMetaGenerator generator = new TextureMetaGenerator.Builder().withSources(tracker.getTouchedTextures()).withOutputLocation(rl).build();
                        var supplier = generator.get(new ResourceLocation(rl.getNamespace(), "textures/"+rl.getPath()+".png.mcmeta"), context);
                        if (supplier != null) {
                            try (var reader = new InputStreamReader(supplier.get())) {
                                JsonObject json = DynamicAssetGenerator.GSON.fromJson(reader, JsonObject.class);
                                if (json.has(AnimationMetadataSection.SECTION_NAME)) {
                                    JsonObject animation = GsonHelper.getAsJsonObject(json, AnimationMetadataSection.SECTION_NAME);
                                    section = AnimationMetadataSection.SERIALIZER.fromJson(animation);
                                }
                            } catch (IOException | JsonParseException e) {
                                DynamicAssetGenerator.LOGGER.warn("Failed to generate texture meta for sprite source "+location()+" at "+rl+": ", e);
                            }
                        }
                    }
                    return new SpriteContents(rl, new FrameSize(image.getWidth(), image.getHeight()), image, section);
                } catch (IOException e) {
                    DynamicAssetGenerator.LOGGER.error("Failed to generate texture for sprite source "+location()+" at "+rl+": ", e);
                    return null;
                }
            });
        });
    }

    @Override
    @NotNull
    public SpriteSourceType type() {
        return SpriteSourcesAccessor.getTypes().get(LOCATION);
    }
}
