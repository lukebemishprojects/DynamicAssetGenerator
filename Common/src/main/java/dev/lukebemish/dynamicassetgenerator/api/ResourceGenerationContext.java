/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import com.google.common.base.Suppliers;
import dev.lukebemish.dynamicassetgenerator.impl.EmptyResourceSource;
import dev.lukebemish.dynamicassetgenerator.impl.ResourceFinder;
import dev.lukebemish.dynamicassetgenerator.impl.platform.Services;
import dev.lukebemish.dynamicassetgenerator.impl.util.InvisibleProviderUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Information available during resource generation, passed to {@link InputStreamSource} as they are generated.
 */
public interface ResourceGenerationContext {

    /**
     * @return a resource location unique to the {@link ResourceCache} this context is linked to
     */
    @NonNull ResourceLocation getCacheName();

    /**
     * @return a tool to access resources during generation
     */
    default ResourceSource getResourceSource() {
        return EmptyResourceSource.INSTANCE;
    }

    /**
     * @return a generation context identical to the current one, but with a different resource source
     */
    @ApiStatus.NonExtendable
    default ResourceGenerationContext withResourceSource(ResourceSource source) {
        ResourceGenerationContext outer = this;
        return new ResourceGenerationContext() {
            @Override
            public @NonNull ResourceLocation getCacheName() {
                return outer.getCacheName();
            }

            @Override
            public ResourceSource getResourceSource() {
                return source;
            }
        };
    }

    interface ResourceSource {
        /**
         * @return a resource source which can read no resources
         */
        static ResourceSource blind() {
            return EmptyResourceSource.INSTANCE;
        }

        /**
         * Attempts to get a resource at a given location, from the highest priority pack not provided by a {@link ResourceCache}.
         *
         * @param location the location to get the resource at
         * @return a supplier for an input stream for the resource, or null if the resource does not exist
         */
        @Nullable IoSupplier<InputStream> getResource(@NonNull ResourceLocation location);

        /**
         * Lists all resources within a given path, from highest to lowest priority.
         *
         * @param location the location to list resources from
         * @return a list of suppliers for input streams for the resources
         */
        List<IoSupplier<InputStream>> getResourceStack(@NonNull ResourceLocation location);

        /**
         * Lists all resources in a namespace that match a given filter, from the highest priority pack not provided by a {@link ResourceCache}.
         *
         * @param path the path to list resources in
         * @param filter a filter to apply to the resource locations
         * @return a map of resource locations to suppliers for input streams for the resources
         */
        Map<ResourceLocation, IoSupplier<InputStream>> listResources(@NonNull String path, @NonNull Predicate<ResourceLocation> filter);

        /**
         * Lists all resources within a given path, from highest to lowest priority.
         * @param path the path to list resources in
         * @param filter a filter to apply to the resource locations
         * @return a map of resource locations to suppliers for input streams for the resources
         */
        Map<ResourceLocation, List<IoSupplier<InputStream>>> listResourceStacks(@NonNull String path, @NonNull Predicate<ResourceLocation> filter);

        /**
         * @return a set of all namespaces that have resources in this context
         */
        @SuppressWarnings("unused")
        @NonNull Set<String> getNamespaces();

        /**
         * A default implementation which exposes resources captured during pack load. Should <em>not</em> be accessed
         * before packs are available, and must be reset every pack reload by reconstructing the listener.
         * @param allowedPacks which packs to select from the captured packs; useful to avoid recursive generation
         * @param type the type of captured pack to target
         * @return a resource source based on the captured packs available when first invoked
         */
        static ResourceGenerationContext.ResourceSource filtered(Predicate<String> allowedPacks, PackType type) {
            int ordinal = type.ordinal();
            Supplier<Stream<PackResources>> packs = () -> ResourceFinder.INSTANCES[ordinal].getPacks().filter(pack -> allowedPacks.test(pack.packId()));
            return of(type, packs);
        }

        /**
         * A default implementation which uses a lazy-loaded list of packs. Should be discarded and re-created when the
         * underlying list would change.
         * @param allowedPacks which packs to select from the captured packs; useful to avoid recursive generation
         * @param type the type of pack to target
         * @param packResources supplies a list of packs to provide resources from
         * @return a resource based on the supplied packs available when first invoked
         */
        static ResourceGenerationContext.ResourceSource filtered(Predicate<String> allowedPacks, PackType type, Supplier<Stream<PackResources>> packResources) {
            Supplier<Stream<PackResources>> packs = () -> packResources.get().filter(pack -> allowedPacks.test(pack.packId()));
            return of(type, packs);
        }

        /**
         * Creates a source which checks the provided fallback source if a resource is not found in this source.
         * @param fallback the source to check if a resource is not found in this source
         * @return a resource source that falls back to the provided source
         */
        default ResourceGenerationContext.ResourceSource fallback(ResourceGenerationContext.ResourceSource fallback) {
            var outer = this;
            return new ResourceSource() {
                @Override
                public @Nullable IoSupplier<InputStream> getResource(@NonNull ResourceLocation location) {
                    IoSupplier<InputStream> supplier = outer.getResource(location);
                    return supplier != null ? supplier : fallback.getResource(location);
                }

                @Override
                public List<IoSupplier<InputStream>> getResourceStack(@NonNull ResourceLocation location) {
                    var list = outer.getResourceStack(location);
                    if (list.isEmpty()) {
                        return fallback.getResourceStack(location);
                    }
                    return list;
                }

                @Override
                public Map<ResourceLocation, IoSupplier<InputStream>> listResources(@NonNull String path, @NonNull Predicate<ResourceLocation> filter) {
                    var map = new HashMap<>(fallback.listResources(path, filter));
                    map.putAll(outer.listResources(path, filter));
                    return map;
                }

                @Override
                public Map<ResourceLocation, List<IoSupplier<InputStream>>> listResourceStacks(@NonNull String path, @NonNull Predicate<ResourceLocation> filter) {
                    var map = new HashMap<>(fallback.listResourceStacks(path, filter));
                    outer.listResourceStacks(path, filter).forEach((rl, list) -> {
                        if (!list.isEmpty()) {
                            map.put(rl, list);
                        }
                    });
                    return map;
                }

                @Override
                public @NonNull Set<String> getNamespaces() {
                    var set = new HashSet<>(fallback.getNamespaces());
                    set.addAll(outer.getNamespaces());
                    return set;
                }
            };
        }

        /**
         * A default implementation which uses a lazy-loaded list of packs. Should be discarded and re-created when the
         * underlying list would change.
         * @param type the type of pack to target
         * @param resources supplies a list of packs to provide resources from
         * @return a resource based on the supplied packs available when first invoked
         */
        static ResourceGenerationContext.ResourceSource of(PackType type, Supplier<Stream<PackResources>> resources) {
            Supplier<List<PackResources>> packs = Suppliers.memoize(() -> Services.DEGROUPER.unpackPacks(Stream.concat(
                resources.get(),
                InvisibleProviderUtils.INVISIBLE_RESOURCE_PROVIDERS.stream().map(InvisibleProviderUtils::constructPlaceholderResourcesFromProvider)
            )).toList());
            return new ResourceGenerationContext.ResourceSource() {

                @Override
                public @Nullable IoSupplier<InputStream> getResource(@NonNull ResourceLocation location) {
                    for (PackResources pack : packs.get()) {
                        IoSupplier<InputStream> resource = pack.getResource(type, location);
                        if (resource != null) {
                            return resource;
                        }
                    }
                    return null;
                }

                @Override
                public List<IoSupplier<InputStream>> getResourceStack(@NonNull ResourceLocation location) {
                    List<IoSupplier<InputStream>> out = new ArrayList<>();
                    for (PackResources pack : packs.get()) {
                        IoSupplier<InputStream> resource = pack.getResource(type, location);
                        if (resource != null) {
                            out.add(resource);
                        }
                    }
                    return out;
                }

                @Override
                public Map<ResourceLocation, IoSupplier<InputStream>> listResources(@NonNull String path, @NonNull Predicate<ResourceLocation> filter) {
                    Map<ResourceLocation, IoSupplier<InputStream>> resources = new HashMap<>();
                    for (PackResources pack : packs.get()) {
                        for (String namespace : pack.getNamespaces(type)) {
                            pack.listResources(type, namespace, path, (rl, s) -> {
                                if (filter.test(rl)) {
                                    if (!resources.containsKey(rl)) {
                                        resources.put(rl, s);
                                    }
                                }
                            });
                        }
                    }
                    return resources;
                }

                @Override
                public Map<ResourceLocation, List<IoSupplier<InputStream>>> listResourceStacks(@NonNull String path, @NonNull Predicate<ResourceLocation> filter) {
                    Map<ResourceLocation, List<IoSupplier<InputStream>>> resources = new HashMap<>();
                    for (PackResources pack : packs.get()) {
                        for (String namespace : pack.getNamespaces(type)) {
                            pack.listResources(type, namespace, path, (rl, s) -> {
                                if (filter.test(rl)) {
                                    List<IoSupplier<InputStream>> list = resources.computeIfAbsent(rl, location -> new ArrayList<>());
                                    list.add(s);
                                }
                            });
                        }
                    }
                    return resources;
                }

                @Override
                public @NonNull Set<String> getNamespaces() {
                    Set<String> namespaces = new HashSet<>();
                    for (PackResources pack : packs.get()) {
                        namespaces.addAll(pack.getNamespaces(type));
                    }
                    return namespaces;
                }
            };
        }
    }
}
