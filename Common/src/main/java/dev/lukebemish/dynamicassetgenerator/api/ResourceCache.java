/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.Timing;
import dev.lukebemish.dynamicassetgenerator.impl.util.ResourceUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;

/**
 * Caches instructions for producing resources, and generates them as packs are loaded.
 */
public abstract class ResourceCache {
    protected List<Supplier<? extends PathAwareInputStreamSource>> cache = new ArrayList<>();
    private final List<Resettable> resetListeners = new ArrayList<>();

    /**
     * Register a new resource cache.
     * @param cache the cache to register
     * @param position the position to register the cache at
     * @return the registered cache
     * @param <T> the type of the cache
     */
    public static <T extends ResourceCache> T register(T cache, Pack.Position position) {
        DynamicAssetGenerator.registerCache(cache.getName(), cache, position);
        return cache;
    }

    /**
     * @return names of other {@link ResourceCache}s that this cache is allowed to use during generation
     */
    public Set<ResourceLocation> getDependencies() {
        return Set.of();
    }

    /**
     * Determines whether this cache can access the provided resources during resource generation
     * @param packId the name of the pack to check whether access is allowed
     * @return true if access is allowed; false otherwise
     */
    public final boolean allowAccess(String packId) {
        String prefix = DynamicAssetGenerator.MOD_ID + "/";
        if (!packId.startsWith(prefix)) {
            return true;
        }
        String remainder = packId.substring(prefix.length());
        @Nullable ResourceLocation targetName = ResourceLocation.read(remainder).result().orElse(null);
        return targetName == null || getDependencies().contains(targetName);
    }

    /**
     * Register a new resource cache as the lowest priority resource pack.
     * @param cache the cache to register
     * @return the registered cache
     * @param <T> the type of the cache
     */
    @SuppressWarnings("unused")
    public static <T extends ResourceCache> T register(T cache) {
        return register(cache, Pack.Position.BOTTOM);
    }

    private final ResourceLocation name;

    /**
     * @return a unique identifier for this cache
     */
    public ResourceLocation getName() {
        return name;
    }

    /**
     * @param name a unique identifier for this cache
     */
    public ResourceCache(ResourceLocation name) {
        this.name = name;
    }

    /**
     * @return a map of all resources this pack can generate; calling this may resolve any given source cached to it
     */
    public Map<ResourceLocation, IoSupplier<InputStream>> getResources() {
        Map<ResourceLocation, IoSupplier<InputStream>> outputs = new HashMap<>();
        this.cache.forEach(p-> {
            try {
                PathAwareInputStreamSource source = p.get();
                Set<ResourceLocation> rls = source.getLocations(makeContext(false));
                if (DynamicAssetGenerator.TIME_RESOURCES) {
                    rls.forEach(rl -> {
                        long startTime = System.nanoTime();
                        outputs.put(rl, ResourceUtils.wrapSafeData(rl, source, makeContext(false)));
                        long endTime = System.nanoTime();

                        long duration = (endTime - startTime)/1000;
                        Timing.recordPartialTime(this.getName().toString(), rl, duration);
                    });
                } else {
                    rls.forEach(rl -> outputs.put(rl, ResourceUtils.wrapSafeData(rl, source, makeContext(false))));
                }
            } catch (Throwable e) {
                DynamicAssetGenerator.LOGGER.error("Issue setting up PathAwareInputStreamSource:",e);
            }
        });

        return outputs;
    }

    private ResourceGenerationContext.ResourceSource filteredSource = null;

    /**
     * Creates a context for generating resources within this cache.
     * @param blind if true, the context should not be able to read resources. Must be true if the available resources
     *              are not guaranteed to be set up yet
     * @return a context for generating resources within this cache
     */
    @NonNull
    public ResourceGenerationContext makeContext(boolean blind) {
        return new ResourceGenerationContext() {
            @Override
            public @NonNull ResourceLocation getCacheName() {
                return getName();
            }

            @Override
            public ResourceSource getResourceSource() {
                if (blind || filteredSource == null) {
                    return ResourceSource.blind();
                }
                return filteredSource;
            }
        };
    }

    /**
     * Adds a listener to be called when this cache is reset.
     * @param listener the listener to add
     */
    @SuppressWarnings("unused")
    public void planResetListener(Resettable listener) {
        this.resetListeners.add(listener);
    }

    /**
     * Resets all listeners associated with this cache.
     */
    @SuppressWarnings("unused")
    public void reset(ResourceGenerationContext context) {
        this.resetListeners.forEach(r -> r.reset(context));
        this.filteredSource = ResourceGenerationContext.ResourceSource.filtered(this::allowAccess, getPackType());
    }

    /**
     * Plan a source to be generated at a given resource location when this cache is resolved.
     * @param rl the resource location the generated source is located at
     * @param source the source to generate
     */
    @SuppressWarnings("unused")
    public void planSource(ResourceLocation rl, InputStreamSource source) {
        cache.add(wrap(()->Set.of(rl),source));
    }

    /**
     * Plan to generate sources at a currently unresolved set of locations when this cache is resolved.
     * @param locations the locations to generate; resolved when this cache is resolved
     * @param source the source to generate
     */
    @SuppressWarnings("unused")
    public void planSource(Supplier<Set<ResourceLocation>> locations, InputStreamSource source) {
        cache.add(wrap(locations, source));
    }

    /**
     * Plan to generate sources at a set of locations when this cache is resolved.
     * @param locations the locations to generate
     * @param source the source to generate
     */
    @SuppressWarnings("unused")
    public void planSource(Set<ResourceLocation> locations, InputStreamSource source) {
        cache.add(wrap(()->locations, source));
    }

    /**
     * Plan to generate a source when this cache is resolved.
     * @param source the source to generate
     */
    public void planSource(PathAwareInputStreamSource source) {
        cache.add(()->source);
        if (source instanceof Resettable resettable)
            planResetListener(resettable);
    }

    /**
     * Plan to generate a currently unresulved source when this cache is resolved.
     * @param source the source to generate; resulved when this cache is resolved
     */
    public void planSource(Supplier<? extends PathAwareInputStreamSource> source) {
        cache.add(source);
    }

    /**
     * @return the type of pack this cache will generate resources for
     */
    @NonNull
    public abstract PackType getPackType();


    private static Supplier<PathAwareInputStreamSource> wrap(Supplier<Set<ResourceLocation>> rls, InputStreamSource source) {
        return () -> new PathAwareInputStreamSource() {
            @Override
            public @NonNull Set<ResourceLocation> getLocations(ResourceGenerationContext context) {
                return rls.get();
            }

            @Override
            public IoSupplier<InputStream> get(ResourceLocation outRl, ResourceGenerationContext context) {
                return source.get(outRl, context);
            }

            @Override
            public @Nullable String createCacheKey(ResourceLocation outRl, ResourceGenerationContext context) {
                return source.createCacheKey(outRl, context);
            }
        };
    }
}
