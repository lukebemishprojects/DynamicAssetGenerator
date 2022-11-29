/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.quilt.compat;

import com.google.auto.service.AutoService;
import dev.lukebemish.dynamicassetgenerator.api.ConditionalInvisibleResourceProvider;
import dev.lukebemish.dynamicassetgenerator.api.InvisibleResourceProvider;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@AutoService(ConditionalInvisibleResourceProvider.class)
@SuppressWarnings("unused")
public class OwoLibProviderWrapper implements ConditionalInvisibleResourceProvider {

    @Override
    public boolean isAvailable() {
        DynamicAssetGenerator.LOGGER.debug("OwoLibProvider is not currently functional. This can be safely ignored.");
        //return QuiltLoader.isModLoaded("owo");
        return false;
    }

    @Override
    public InvisibleResourceProvider get() {
        return new OwoLibProvider();
    }

    @ParametersAreNonnullByDefault
    public static class OwoLibProvider implements InvisibleResourceProvider {
        private static Map<ResourceLocation, String> tagMap;

        @Override
        public InputStream getResource(PackType type, ResourceLocation location) {
            if (type == PackType.SERVER_DATA) {
                checkMap();
                if (tagMap.containsKey(location))
                    return new ByteArrayInputStream(tagMap.get(location).getBytes());
            }
            return null;
        }

        @Override
        public Collection<ResourceLocation> getResources(PackType type, String namespace, String path, Predicate<ResourceLocation> filter) {
            if (type == PackType.SERVER_DATA) {
                checkMap();
                return tagMap.keySet().stream().filter(location -> location.getNamespace().equals(namespace) && location.getPath().startsWith(path) && filter.test(location)).toList();
            }
            return List.of();
        }

        @Override
        public boolean hasResource(PackType type, ResourceLocation location) {
            if (type == PackType.SERVER_DATA) {
                checkMap();
                return tagMap.containsKey(location);
            }
            return false;
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
                // TODO: Waiting on owo-lib to release something with my PR.
            }
        }
    }
}
