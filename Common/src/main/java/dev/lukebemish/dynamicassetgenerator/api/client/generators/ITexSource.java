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
import dev.lukebemish.dynamicassetgenerator.api.cache.DataConsumer;
import dev.lukebemish.dynamicassetgenerator.impl.client.ClientRegisters;
import dev.lukebemish.dynamicassetgenerator.impl.client.TexSourceCachingWrapper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface ITexSource {

    String METADATA_CACHE_KEY = "__dynamic_asset_generator_metadata";
    Codec<ITexSource> CODEC = CacheMetaCodec.of(ExtraCodecs.lazyInitializedCodec(() -> new Codec<Codec<? extends ITexSource>>() {
        @Override
        public <T> DataResult<Pair<Codec<? extends ITexSource>, T>> decode(DynamicOps<T> ops, T input) {
            return ResourceLocation.CODEC.decode(ops, input).flatMap(keyValuePair -> !ClientRegisters.ITEXSOURCES.containsKey(keyValuePair.getFirst())
                    ? DataResult.error(() -> "Unknown dynamic texture source type: " + keyValuePair.getFirst())
                    : DataResult.success(keyValuePair.mapFirst(ClientRegisters.ITEXSOURCES::get)));
        }

        @Override
        public <T> DataResult<T> encode(Codec<? extends ITexSource> input, DynamicOps<T> ops, T prefix) {
            ResourceLocation key = ClientRegisters.ITEXSOURCES.inverse().get(input);
            if (key == null) {
                return DataResult.error(() -> "Unregistered dynamic texture source type: " + input);
            }
            T toMerge = ops.createString(key.toString());
            return ops.mergeToPrimitive(prefix, toMerge);
        }
    }).dispatch(ITexSource::codec, Function.identity()).xmap(ITexSource::cached, wrapped -> {
        while (wrapped instanceof TexSourceCachingWrapper cachingWrapper) {
            wrapped = cachingWrapper.wrapped();
        }
        return wrapped;
    }), new DataConsumer<>() {
        @Override
        @NotNull
        public <T1> DataResult<T1> encode(DynamicOps<T1> ops, TexSourceDataHolder data, ITexSource object) {
            return object.cacheMetadata(ops, data);
        }
    }, METADATA_CACHE_KEY, TexSourceDataHolder.class);

    static <T extends ITexSource> void register(ResourceLocation rl, Codec<T> codec) {
        ClientRegisters.ITEXSOURCES.put(rl, codec);
    }

    /**
     * If your texture source depends on runtime data context (anything stored in the {@link TexSourceDataHolder}), you will
     * need to override this method to return a unique key for the given context. This key will be used when caching
     * textures generated by this source. If your source should not be cached, return a DataResult.error.
     * @param ops DynamicOps to encode the unique key with.
     * @param data Data holder that the key is dependent on.
     * @return A success with a unique key for the given context, or an error if no key can be generated.
     */
    @NotNull @ApiStatus.Experimental
    default <T> DataResult<T> cacheMetadata(DynamicOps<T> ops, TexSourceDataHolder data) {
        return DataResult.success(ops.empty());
    }

    Codec<? extends ITexSource> codec();

    @Nullable
    IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context);

    @ApiStatus.Experimental
    default ITexSource cached() {
        if (this instanceof TexSourceCachingWrapper) return this;
        return new TexSourceCachingWrapper(this);
    }
}
