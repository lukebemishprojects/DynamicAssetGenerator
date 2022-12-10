/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.quilt.compat;

import com.google.auto.service.AutoService;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.dynamicassetgenerator.api.ConditionalInvisibleResourceProvider;
import dev.lukebemish.dynamicassetgenerator.api.InvisibleResourceProvider;
import dev.lukebemish.dynamicassetgenerator.api.templates.TagFile;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import io.wispforest.owo.util.TagInjector;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.quiltmc.loader.api.QuiltLoader;

import javax.annotation.ParametersAreNonnullByDefault;
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

    @ParametersAreNonnullByDefault
    public static class OwoLibProvider implements InvisibleResourceProvider {
        private static Map<ResourceLocation, String> tagMap;

        @Override
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
            if (type == PackType.SERVER_DATA) {
                checkMap();
                if (tagMap.containsKey(location))
                    return () -> new ByteArrayInputStream(tagMap.get(location).getBytes());
            }
            return null;
        }

        @Override
        public void listResources(PackType type, String namespace, String path, PackResources.ResourceOutput resourceOutput) {
            if (type == PackType.SERVER_DATA) {
                checkMap();
                tagMap.keySet().stream()
                        .filter(location -> location.getNamespace().equals(namespace) && location.getPath().startsWith(path))
                        .forEach(rl -> resourceOutput.accept(rl, this.getResource(type, rl)));
            }
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            if (type == PackType.SERVER_DATA)
                return tagMap.keySet().stream().map(ResourceLocation::getNamespace).collect(Collectors.toSet());
            else return Set.of();
        }

        @Override
        public void reset(PackType type) {
            tagMap = null;
        }

        private void checkMap() {
            if (tagMap == null) {
                tagMap = new HashMap<>();
                TagInjector.getInjections().forEach((key, values) -> {
                    var tag = new TagFile(new ArrayList<>(values), false);
                    JsonElement encoded;
                    try {
                        encoded =
                                TagFile.CODEC.encodeStart(JsonOps.INSTANCE, tag).getOrThrow(false, s -> {});
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
