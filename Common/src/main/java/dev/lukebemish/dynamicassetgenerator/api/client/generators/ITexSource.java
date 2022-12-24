/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.cache.CacheMetaCodec;
import dev.lukebemish.dynamicassetgenerator.impl.client.ClientRegisters;
import dev.lukebemish.dynamicassetgenerator.impl.client.TexSourceCachingWrapper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface ITexSource {
    Codec<ITexSource> CODEC = ExtraCodecs.lazyInitializedCodec(() -> new Codec<Codec<? extends ITexSource>>() {
        @Override
        public <T> DataResult<Pair<Codec<? extends ITexSource>, T>> decode(DynamicOps<T> ops, T input) {
            return ResourceLocation.CODEC.decode(ops, input).flatMap(keyValuePair -> !ClientRegisters.ITEXSOURCES.containsKey(keyValuePair.getFirst())
                    ? DataResult.error("Unknown dynamic texture source type: " + keyValuePair.getFirst())
                    : DataResult.success(keyValuePair.mapFirst(ClientRegisters.ITEXSOURCES::get)));
        }

        @Override
        public <T> DataResult<T> encode(Codec<? extends ITexSource> input, DynamicOps<T> ops, T prefix) {
            ResourceLocation key = ClientRegisters.ITEXSOURCES.inverse().get(input);
            if (key == null)
            {
                return DataResult.error("Unregistered dynamic texture source type: " + input);
            }
            T toMerge = ops.createString(key.toString());
            return ops.mergeToPrimitive(prefix, toMerge);
        }
    }).dispatch((ITexSource source) -> {
        var codec = source.codec();
        var found = ClientRegisters.ITEXSOURCES_WRAPPED.get(codec);
        if (found != null) return found;
        return codec;
    }, Function.identity()).xmap(TexSourceCachingWrapper::new, wrapped -> {
        while (wrapped instanceof TexSourceCachingWrapper cachingWrapper) {
            wrapped = cachingWrapper.wrapped();
        }
        return wrapped;
    });

    String METADATA_CACHE_KEY = "__dynamic_asset_generator_metadata";

    static <T extends ITexSource> void register(ResourceLocation rl, Codec<T> codec) {
        CacheMetaCodec<T, TexSourceDataHolder> wrappedCodec = new CacheMetaCodec<>(codec, new CacheMetaCodec.DataConsumer<>() {
            @Override
            @NotNull
            public <T1> DataResult<T1> encode(DynamicOps<T1> ops, TexSourceDataHolder data, T object) {
                return object.cacheMetadata(ops, data);
            }
        }, METADATA_CACHE_KEY, TexSourceDataHolder.class);
        ClientRegisters.ITEXSOURCES.put(rl, wrappedCodec);
        ClientRegisters.ITEXSOURCES_WRAPPED.put(codec, wrappedCodec);
    }

    @NotNull default <T> DataResult<T> cacheMetadata(DynamicOps<T> ops, TexSourceDataHolder data) {
        return DataResult.success(ops.empty());
    }

    Codec<? extends ITexSource> codec();

    @Nullable
    IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context);
}
