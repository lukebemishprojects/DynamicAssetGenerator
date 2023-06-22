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
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Supplier;

public abstract class ResourceCache {
    protected static final String SOURCE_JSON_DIR = DynamicAssetGenerator.MOD_ID;
    protected List<Supplier<? extends PathAwareInputStreamSource>> cache = new ArrayList<>();
    private final List<Resettable> resetListeners = new ArrayList<>();

    public static <T extends ResourceCache> T register(T cache, Pack.Position position) {
        DynamicAssetGenerator.registerCache(cache.getName(), cache, position);
        return cache;
    }

    @SuppressWarnings("unused")
    public static <T extends ResourceCache> T register(T cache) {
        return register(cache, Pack.Position.BOTTOM);
    }

    private final ResourceLocation name;

    public ResourceLocation getName() {
        return name;
    }

    public ResourceCache(ResourceLocation name) {
        this.name = name;
    }

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

    @NotNull
    public ResourceGenerationContext getContext() {
        return OldResourceGenerationContext.make(this.name, this.getPackType());
    }

    @SuppressWarnings("unused")
    public void planResetListener(Resettable listener) {
        this.resetListeners.add(listener);
    }

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

    public abstract boolean shouldCache();

    public Path cachePath() {
        return Services.PLATFORM.getModDataFolder().resolve("cache").resolve(name.getNamespace()).resolve(name.getPath());
    }

    @SuppressWarnings("unused")
    public void planSource(ResourceLocation rl, InputStreamSource source) {
        cache.add(wrap(()->Set.of(rl),source));
    }

    @SuppressWarnings("unused")
    public void planSource(Supplier<Set<ResourceLocation>> locations, InputStreamSource source) {
        cache.add(wrap(locations, source));
    }

    @SuppressWarnings("unused")
    public void planSource(Set<ResourceLocation> locations, InputStreamSource source) {
        cache.add(wrap(()->locations, source));
    }

    public void planSource(PathAwareInputStreamSource source) {
        cache.add(()->source);
        if (source instanceof Resettable resettable)
            planResetListener(resettable);
    }

    public void planSource(Supplier<? extends PathAwareInputStreamSource> source) {
        cache.add(source);
    }

    @NotNull
    public abstract PackType getPackType();

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
