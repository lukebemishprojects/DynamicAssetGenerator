/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.IntStream;


/**
 * Wraps a resource source and caches the results of all reads. This means that resources will only be read once so long
 * as this wrapper persists.
 */
@ApiStatus.Experimental
public final class CachingResourceSource implements ResourceGenerationContext.ResourceSource {

    private final ResourceGenerationContext.ResourceSource delegate;
    private final Map<ResourceLocation, List<Either<byte[], IOException>>> cache = new ConcurrentHashMap<>();

    private CachingResourceSource(ResourceGenerationContext.ResourceSource delegate) {
        this.delegate = delegate;
    }

    public static CachingResourceSource of(ResourceGenerationContext.ResourceSource delegate) {
        return new CachingResourceSource(delegate);
    }

    private List<IoSupplier<InputStream>> wrap(ResourceLocation location, List<IoSupplier<InputStream>> streams) {
        if (streams.isEmpty()) {
            return List.of();
        }
        IoIntFunction<InputStream> wrapper = idx -> {
            var bytes = cache.computeIfAbsent(location, loc -> {
                List<Either<byte[], IOException>> list = new ArrayList<>();
                for (var stream : streams) {
                    try (var in = stream.get()) {
                        list.add(Either.left(in.readAllBytes()));
                    } catch (IOException e) {
                        list.add(Either.right(e));
                    }
                }
                return list;
            });
            var either = bytes.get(idx);
            if (either.left().isPresent()) {
                return new ByteArrayInputStream(either.left().get());
            } else {
                //noinspection OptionalGetWithoutIsPresent
                throw either.right().get();
            }
        };
        return IntStream.range(0, streams.size()).<IoSupplier<InputStream>>mapToObj(i -> () -> wrapper.get(i)).toList();
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(@NotNull ResourceLocation location) {
        List<IoSupplier<InputStream>> streams = wrap(location, delegate.getResourceStack(location));
        if (streams.isEmpty()) {
            return null;
        }
        return streams.get(0);
    }

    @Override
    public List<IoSupplier<InputStream>> getResourceStack(@NotNull ResourceLocation location) {
        return wrap(location, delegate.getResourceStack(location));
    }

    @Override
    public Map<ResourceLocation, IoSupplier<InputStream>> listResources(@NotNull String path, @NotNull Predicate<ResourceLocation> filter) {
        var delegated = delegate.listResourceStacks(path, filter);
        var out = new HashMap<ResourceLocation, IoSupplier<InputStream>>();
        delegated.forEach((location, streams) -> {
            var wrapped = wrap(location, streams);
            if (!wrapped.isEmpty()) {
                out.put(location, wrapped.get(0));
            }
        });
        return out;
    }

    @Override
    public Map<ResourceLocation, List<IoSupplier<InputStream>>> listResourceStacks(@NotNull String path, @NotNull Predicate<ResourceLocation> filter) {
        var delegated = delegate.listResourceStacks(path, filter);
        var out = new HashMap<ResourceLocation, List<IoSupplier<InputStream>>>();
        delegated.forEach((location, streams) -> {
            var wrapped = wrap(location, streams);
            if (!wrapped.isEmpty()) {
                out.put(location, wrapped);
            }
        });
        return out;
    }

    @Override
    public @NotNull Set<String> getNamespaces() {
        return delegate.getNamespaces();
    }

    private interface IoIntFunction<T> {
        T get(int idx) throws IOException;
    }
}
