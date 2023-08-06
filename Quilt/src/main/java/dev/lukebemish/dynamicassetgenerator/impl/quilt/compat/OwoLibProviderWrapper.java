/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.quilt.compat;

import com.google.auto.service.AutoService;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.dynamicassetgenerator.api.compat.ConditionalInvisibleResourceProvider;
import dev.lukebemish.dynamicassetgenerator.api.compat.InvisibleResourceProvider;
import dev.lukebemish.dynamicassetgenerator.api.templates.TagFile;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import io.wispforest.owo.util.TagInjector;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.loader.api.QuiltLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(ConditionalInvisibleResourceProvider.class)
@SuppressWarnings("unused")
public class OwoLibProviderWrapper implements ConditionalInvisibleResourceProvider {

    @Override
    public boolean isAvailable() {
        return QuiltLoader.isModLoaded("owo");
    }

    @Override
    public InvisibleResourceProvider get() {
        return new OwoLibProvider();
    }

    public static class OwoLibProvider implements InvisibleResourceProvider {
        private static Map<ResourceLocation, String> tagMap;

        @Override
        public IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location) {
            if (type == PackType.SERVER_DATA) {
                checkMap();
                if (tagMap.containsKey(location))
                    return () -> new ByteArrayInputStream(tagMap.get(location).getBytes());
            }
            return null;
        }

        @Override
        public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String path, PackResources.@NotNull ResourceOutput resourceOutput) {
            if (type == PackType.SERVER_DATA) {
                checkMap();
                tagMap.keySet().stream()
                        .filter(location -> location.getNamespace().equals(namespace) && location.getPath().startsWith(path))
                        .forEach(rl -> resourceOutput.accept(rl, this.getResource(type, rl)));
            }
        }

        @Override
        public Set<String> getNamespaces(@NotNull PackType type) {
            if (type == PackType.SERVER_DATA) {
                checkMap();
                return tagMap.keySet().stream().map(ResourceLocation::getNamespace).collect(Collectors.toSet());
            }
            else return Set.of();
        }

        @Override
        public void reset(@NotNull PackType type) {
            if (type == PackType.SERVER_DATA)
                tagMap = null;
        }

        private void checkMap() {
            if (tagMap == null) {
                synchronized (this) {
                    if (tagMap != null) return;
                    tagMap = new HashMap<>();
                    TagInjector.getInjections().forEach((key, values) -> {
                        var tag = new TagFile(new ArrayList<>(values), false);
                        JsonElement encoded;
                        try {
                            encoded =
                                    TagFile.CODEC.encodeStart(JsonOps.INSTANCE, tag).getOrThrow(false, s -> {
                                    });
                        } catch (RuntimeException e) {
                            DynamicAssetGenerator.LOGGER.error("Error encoding tag file from OwoLib entries: " + e.getMessage());
                            return;
                        }
                        tagMap.put(new ResourceLocation(key.tagId().getNamespace(), "tags/" + key.type() + "/" + key.tagId().getPath() + ".json"),
                                DynamicAssetGenerator.GSON.toJson(encoded));
                    });
                }
            }
        }
    }
}
