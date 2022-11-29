/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.util;

import dev.lukebemish.dynamicassetgenerator.api.ConditionalInvisibleResourceProvider;
import dev.lukebemish.dynamicassetgenerator.api.InvisibleResourceProvider;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
public final class InvisibleProviderUtils {
    public static final List<InvisibleResourceProvider> INVISIBLE_RESOURCE_PROVIDERS =
            Stream.concat(
                    ServiceLoader.load(InvisibleResourceProvider.class).stream()
                            .map(ServiceLoader.Provider::get),
                    ServiceLoader.load(ConditionalInvisibleResourceProvider.class).stream()
                            .map(ServiceLoader.Provider::get)
                            .filter(ConditionalInvisibleResourceProvider::isAvailable)
                            .map(ConditionalInvisibleResourceProvider::get)
            ).toList();

    static {
        DynamicAssetGenerator.LOGGER.debug("Loaded invisible resource providers: {}", InvisibleProviderUtils.INVISIBLE_RESOURCE_PROVIDERS);
    }

    private InvisibleProviderUtils() {}

    public static PackResources constructPlaceholderResourcesFromProvider(InvisibleResourceProvider provider) {
        return new PackResources() {
            @Nullable
            @Override
            public InputStream getRootResource(String fileName) {
                return null;
            }

            @Override
            public InputStream getResource(PackType type, ResourceLocation location) {
                return provider.getResource(type, location);
            }

            @Override
            public Collection<ResourceLocation> getResources(PackType type, String namespace, String path, Predicate<ResourceLocation> filter) {
                return provider.getResources(type, namespace, path, filter);
            }

            @Override
            public boolean hasResource(PackType type, ResourceLocation location) {
                return provider.hasResource(type, location);
            }

            @Override
            public Set<String> getNamespaces(PackType type) {
                return provider.getNamespaces(type);
            }

            @Nullable
            @Override
            public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
                return null;
            }

            @Override
            public String getName() {
                return "placeholder__"+provider.getClass().getName().toLowerCase(Locale.ROOT)
                        .replace('.', '_')
                        .replace('$', '_');
            }

            @Override
            public void close() {}
        };
    }
}
