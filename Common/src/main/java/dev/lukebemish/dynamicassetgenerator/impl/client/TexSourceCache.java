/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.cache.CacheMetaJsonOps;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.impl.CacheReference;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TexSourceCache {
    private TexSourceCache() {
    }

    private static final Map<ResourceLocation, Map<String, CacheReference<Either<NativeImage, IOException>>>> MULTI_CACHE = new ConcurrentHashMap<>();

    @NotNull public static NativeImage fromCache(IoSupplier<NativeImage> supplier, ITexSource source, ResourceGenerationContext context, TexSourceDataHolder data) throws IOException {
        var cache = MULTI_CACHE.computeIfAbsent(context.cacheName(), k -> new ConcurrentHashMap<>());
        try {
            var dataOps = new CacheMetaJsonOps<>(data, TexSourceDataHolder.class);
            String cacheKey = ITexSource.CODEC.encodeStart(dataOps, source).result().map(DynamicAssetGenerator.GSON_FLAT::toJson).orElse(null);
            if (cacheKey == null) {
                return supplier.get();
            }
            var ref = cache.computeIfAbsent(cacheKey, key -> new CacheReference<>());
            var result = ref.calcSync(cached -> {
                if (cached == null) {
                    try {
                        NativeImage image = supplier.get();
                        ref.setHeld(Either.left(image));
                        NativeImage output = NativeImageHelper.of(image.format(), image.getHeight(), image.getWidth(), false);
                        output.copyFrom(image);
                        return Either.left(output);
                    } catch (IOException e) {
                        ref.setHeld(Either.right(e));
                        return Either.right(e);
                    }
                } else if (cached.left().isPresent()) {
                    NativeImage image = cached.left().get();
                    NativeImage output = NativeImageHelper.of(image.format(), image.getHeight(), image.getWidth(), false);
                    output.copyFrom(image);
                    return Either.left(output);
                } else {
                    return Either.right(cached.right().get());
                }
            });
            if (result.left().isPresent()) {
                return result.left().get();
            } else {
                throw result.right().get();
            }
        } catch (RuntimeException e) {
            throw new IOException("Could not cache texture source; it may not have a registered codec.", e);
        }
    }

    public static void reset(ResourceGenerationContext context) {
        synchronized (MULTI_CACHE) {
            Map<String, CacheReference<Either<NativeImage, IOException>>> cache;
            if ((cache = MULTI_CACHE.get(context.cacheName())) != null) {
                cache.forEach((s, e) -> {
                    if (e.getHeld().left().isPresent()) {
                        e.getHeld().left().get().close();
                    }
                });
                MULTI_CACHE.remove(context.cacheName());
            }
        }
    }

}
