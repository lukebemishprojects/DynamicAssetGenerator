/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl;

import dev.lukebemish.dynamicassetgenerator.api.ResourceCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GeneratedPackResources implements PackResources {

    private final ResourceCache cache;
    private Map<ResourceLocation, IoSupplier<InputStream>> streams;

    public GeneratedPackResources(ResourceCache cache) {
        this.cache = cache;
        cache.reset(cache.makeContext(true));
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    private Map<ResourceLocation, IoSupplier<InputStream>> getStreams() {
        if (streams == null) {
            streams = cache.getResources();
        }
        return streams;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String @NonNull ... strings) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(@NonNull PackType packType, @NonNull ResourceLocation location) {
        if (packType == cache.getPackType()) {
            if (getStreams().containsKey(location)) {
                return getStreams().get(location);
            }
        }
        return null;
    }

    @Override
    public void listResources(@NonNull PackType packType, @NonNull String namespace, @NonNull String directory, @NonNull ResourceOutput resourceOutput) {
        if (packType == cache.getPackType()) {
            for (ResourceLocation key : getStreams().keySet()) {
                if (key.getPath().startsWith(directory) && key.getNamespace().equals(namespace) && getStreams().get(key) != null) {
                    resourceOutput.accept(key, getStreams().get(key));
                }
            }
        }
    }

    @Override
    public @NonNull Set<String> getNamespaces(@NonNull PackType type) {
        Set<String> namespaces = new HashSet<>();
        if (type == cache.getPackType()) {
            for (ResourceLocation key : getStreams().keySet()) {
                namespaces.add(key.getNamespace());
            }
        }
        return namespaces;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        if (deserializer.getMetadataSectionName().equals("pack")) {
            return (T) DynamicAssetGenerator.fromCache(cache);
        }
        return null;
    }

    @Override
    public @NonNull String packId() {
        return DynamicAssetGenerator.MOD_ID+'/'+cache.getName().toString();
    }

    @Override
    public void close() {

    }
}
