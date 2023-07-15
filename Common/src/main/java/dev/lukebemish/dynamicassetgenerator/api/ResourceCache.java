/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import dev.lukebemish.dynamicassetgenerator.impl.Benchmarking;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.OldResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.impl.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Supplier;

/**
 * Caches instructions for producing resources, and generates them as packs are loaded.
 */
public abstract class ResourceCache {
    protected static final String SOURCE_JSON_DIR = DynamicAssetGenerator.MOD_ID;
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
        Map<ResourceLocation, IoSupplier<InputStream>> outputsSetup = new HashMap<>();
        this.cache.forEach(p-> {
            try {
                PathAwareInputStreamSource source = p.get();
                Set<ResourceLocation> rls = source.getLocations();
                rls.forEach(rl -> outputsSetup.put(rl, wrapSafeData(rl, source.get(rl, getContext()))));
            } catch (Throwable e) {
                DynamicAssetGenerator.LOGGER.error("Issue setting up PathAwareInputStreamSource:",e);
            }
        });

        var outputs = outputsSetup;

        if (shouldCache())
            outputs = wrapCachedData(outputs);

        return outputs;
    }

    /**
     * @return a context for generating resources within this cache
     */
    @NotNull
    public ResourceGenerationContext getContext() {
        return OldResourceGenerationContext.make(this.name, this.getPackType());
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
    public void reset() {
        this.resetListeners.forEach(Resettable::reset);
    }

    private IoSupplier<InputStream> wrapSafeData(ResourceLocation rl, IoSupplier<InputStream> supplier) {
        if (supplier == null) return null;
        IoSupplier<InputStream> output = () -> {
            try {
                return supplier.get();
            } catch (Throwable e) {
                DynamicAssetGenerator.LOGGER.error("Issue reading supplying resource {}:", rl, e);
                throw new IOException(e);
            }
        };
        if (DynamicAssetGenerator.TIME_RESOURCES) {
            return () -> {
                long startTime = System.nanoTime();
                var result = output.get();
                long endTime = System.nanoTime();

                long duration = (endTime - startTime)/1000;
                Benchmarking.recordTime(this, rl, duration);
                return result;
            };
        }
        return output;
    }

    private Map<ResourceLocation, IoSupplier<InputStream>> wrapCachedData(Map<ResourceLocation, IoSupplier<InputStream>> map) {
        HashMap<ResourceLocation, IoSupplier<InputStream>> output = new HashMap<>();
        map.forEach((rl, supplier) -> {
            if (supplier == null) return;
            IoSupplier<InputStream> wrapped = () -> {
                try {
                    Path path = this.cachePath().resolve(rl.getNamespace()).resolve(rl.getPath());
                    if (!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
                    if (!Files.exists(path)) {
                        InputStream stream = supplier.get();
                        Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING);
                    }
                    return new BufferedInputStream(Files.newInputStream(path));
                } catch (IOException e) {
                    DynamicAssetGenerator.LOGGER.error("Could not cache resource...", e);
                    throw new IOException(e);
                }
            };
            output.put(rl, wrapped);
        });
        return output;
    }

    /**
     * @return whether this cache should be cached to disk
     */
    public abstract boolean shouldCache();

    /**
     * @return the path on disk to cache this cache to, if it should be cached to the disk
     */
    public Path cachePath() {
        return Services.PLATFORM.getModDataFolder().resolve("cache").resolve(name.getNamespace()).resolve(name.getPath());
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
    @NotNull
    public abstract PackType getPackType();

    /**
     * This method should be considered internal, but to avoid breaking backwards compatibility, no breaking changes
     * will be made until DynAssetGen 5.0.0 or later.
     */
    @ApiStatus.Internal
    public static Supplier<PathAwareInputStreamSource> wrap(Supplier<Set<ResourceLocation>> rls, InputStreamSource source) {
        return () -> new PathAwareInputStreamSource() {
            @Override
            public @NotNull Set<ResourceLocation> getLocations() {
                return rls.get();
            }

            @Override
            public IoSupplier<InputStream> get(ResourceLocation outRl, ResourceGenerationContext context) {
                return source.get(outRl, context);
            }
        };
    }
}
