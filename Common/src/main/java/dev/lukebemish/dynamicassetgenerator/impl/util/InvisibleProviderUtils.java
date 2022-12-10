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
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.Set;
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
            public IoSupplier<InputStream> getRootResource(String... strings) {
                return null;
            }

            @Override
            public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
                return provider.getResource(type, location);
            }

            @Override
            public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {
                provider.listResources(packType, namespace, path, resourceOutput);
            }

            @Override
            public @NotNull Set<String> getNamespaces(PackType type) {
                return provider.getNamespaces(type);
            }

            @Nullable
            @Override
            public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
                return null;
            }

            @Override
            public @NotNull String packId() {
                return "placeholder__"+provider.getClass().getName().toLowerCase(Locale.ROOT)
                        .replace('.', '_')
                        .replace('$', '_');
            }

            @Override
            public void close() {}
        };
    }
}
