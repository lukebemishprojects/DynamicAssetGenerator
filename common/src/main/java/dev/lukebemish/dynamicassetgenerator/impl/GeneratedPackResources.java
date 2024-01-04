/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl;

import dev.lukebemish.dynamicassetgenerator.api.ResourceCache;
import dev.lukebemish.dynamicassetgenerator.impl.util.ReentryDetector;
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
    private @Nullable Map<ResourceLocation, IoSupplier<InputStream>> streams;

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
        try (var lock = getResourceDetector.reentrant(new GetResource(packType, location))) {
            if (lock.reentrant()) {
                return null;
            }
            if (packType == cache.getPackType()) {
                if (getStreams().containsKey(location)) {
                    return getStreams().get(location);
                }
            }
            return null;
        }
    }

    @Override
    public void listResources(@NonNull PackType packType, @NonNull String namespace, @NonNull String directory, @NonNull ResourceOutput resourceOutput) {
        try (var lock = listResourcesDetector.reentrant(new ListResources(packType, namespace, directory))) {
            if (lock.reentrant()) {
                return;
            }
            if (packType == cache.getPackType()) {
                for (ResourceLocation key : getStreams().keySet()) {
                    if (key.getPath().startsWith(directory) && key.getNamespace().equals(namespace) && getStreams().get(key) != null) {
                        resourceOutput.accept(key, getStreams().get(key));
                    }
                }
            }
        }
    }

    @Override
    public @NonNull Set<String> getNamespaces(@NonNull PackType type) {
        try (var lock = getNamespacesDetector.reentrant(type)) {
            if (lock.reentrant()) {
                return Set.of();
            }
            Set<String> namespaces = new HashSet<>();
            if (type == cache.getPackType()) {
                for (ResourceLocation key : getStreams().keySet()) {
                    namespaces.add(key.getNamespace());
                }
            }
            return namespaces;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        if (deserializer.getMetadataSectionName().equals("pack")) {
            return (T) DynamicAssetGenerator.makeMetadata(cache);
        }
        return null;
    }

    @Override
    public @NonNull String packId() {
        return DynamicAssetGenerator.MOD_ID + '/' + cache.getName();
    }

    @Override
    public void close() {

    }

    private record GetResource(@NonNull PackType packType, @NonNull ResourceLocation location) {}
    private final ReentryDetector<GetResource> getResourceDetector = new ReentryDetector<>();

    private record ListResources(@NonNull PackType packType, @NonNull String namespace, @NonNull String directory) {}
    private final ReentryDetector<ListResources> listResourcesDetector = new ReentryDetector<>();

    private final ReentryDetector<PackType> getNamespacesDetector = new ReentryDetector<>();
}
