/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TextureMetaGenerator;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TouchedTextureTracker;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.client.ExposesName;
import dev.lukebemish.dynamicassetgenerator.impl.client.ForegroundExtractor;
import dev.lukebemish.dynamicassetgenerator.impl.client.TexSourceCache;
import dev.lukebemish.dynamicassetgenerator.impl.client.platform.ClientServices;
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
import java.util.function.Supplier;

/**
 * A sprite source which makes use of {@link TexSource}s to provide sprites at resource pack load. May be more reliable
 * than a {@link AssetResourceCache} for generating sprites based off of textures added by other mods which use runtime
 * resource generation techniques.
 */
public interface DynamicSpriteSource extends SpriteSource {
    /**
     * @return a map of texture location, not including the {@code "textures/"} prefix or file extension, to texture source
     */
    Map<ResourceLocation, TexSource> getSources(ResourceGenerationContext context, ResourceManager resourceManager);

    /**
     * @return a unique identifier for this sprite source type
     */
    ResourceLocation getLocation();

    /**
     * Registers a sprite source type.
     * @param location the location which this sprite source type can be referenced from a texture atlas JSON file with
     * @param codec a codec to provide instances of the type
     */
    static void register(ResourceLocation location, Codec<? extends DynamicSpriteSource> codec) {
        ClientServices.PLATFORM_CLIENT.addSpriteSource(location, codec);
    }

    /**
     * Registers a sprite source type
     * @param location the location which this sprite source type can be referenced from a texture atlas JSON file with
     * @param constructor supplies instances of this sprite source type
     */
    static void register(ResourceLocation location, Supplier<? extends DynamicSpriteSource> constructor) {
        ClientServices.PLATFORM_CLIENT.addSpriteSource(location, Codec.unit(constructor));
    }

    /**
     * Will be run before generation starts. Allows for clearing of anything that saves state (caches or the like).
     * Implementations should call the super method to clear texture source and palette transfer caches.
     * @param context context for the generation that will occur after this source is reset
     */
    default void reset(ResourceGenerationContext context) {
        TexSourceCache.reset(context);
        ForegroundExtractor.reset(context);
    }

    @Override
    default void run(ResourceManager resourceManager, SpriteSource.Output output) {
        ResourceGenerationContext context = new ResourceGenerationContext() {
            @Override
            public @NotNull ResourceLocation getCacheName() {
                if (output instanceof ExposesName exposesName) {
                    var atlasName = exposesName.dynamicassetgenerator$getName();
                    return getLocation().withPath(getLocation().getPath()+"__"+atlasName.getNamespace()+"__"+atlasName.getPath());
                }
                return getLocation();
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

        this.reset(context);

        Map<ResourceLocation, TexSource> sources = getSources(context, resourceManager);

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
                                DynamicAssetGenerator.LOGGER.warn("Failed to generate texture meta for sprite source type "+getLocation()+" at "+rl+": ", e);
                            }
                        }
                    }
                    FrameSize frameSize = new FrameSize(image.getWidth(), image.getHeight());
                    if (section != AnimationMetadataSection.EMPTY) {
                        frameSize = section.calculateFrameSize(image.getWidth(), image.getHeight());
                    }
                    return new SpriteContents(rl, frameSize, image, section);
                } catch (IOException e) {
                    DynamicAssetGenerator.LOGGER.error("Failed to generate texture for sprite source type "+getLocation()+" at "+rl+": ", e);
                    return null;
                }
            });
        });
    }

    @Override
    @NotNull
    default SpriteSourceType type() {
        return SpriteSourcesAccessor.getTypes().get(getLocation());
    }
}
