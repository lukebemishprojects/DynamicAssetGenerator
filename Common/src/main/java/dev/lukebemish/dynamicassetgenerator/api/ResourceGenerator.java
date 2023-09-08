/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.lukebemish.dynamicassetgenerator.api.cache.CacheMetaCodec;
import dev.lukebemish.dynamicassetgenerator.api.cache.CacheMetaJsonOps;
import dev.lukebemish.dynamicassetgenerator.api.cache.DataConsumer;
import dev.lukebemish.dynamicassetgenerator.impl.CommonRegisters;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.ResourceCachingData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Represents a set of instructions to generate any number of resources, to be read from JSON.
 */
public interface ResourceGenerator extends PathAwareInputStreamSource {
    String PERSISTENT_CACHE_KEY = "__dynamic_asset_generator_persistent";
    Codec<ResourceGenerator> CODEC = ExtraCodecs.lazyInitializedCodec(() -> new Codec<Codec<? extends ResourceGenerator>>() {
        @Override
        public <T> DataResult<Pair<Codec<? extends ResourceGenerator>, T>> decode(DynamicOps<T> ops, T input) {
            return ResourceLocation.CODEC.decode(ops, input).flatMap(keyValuePair -> !CommonRegisters.RESOURCEGENERATORS.containsKey(keyValuePair.getFirst())
                    ? DataResult.error(() -> "Unknown dynamic resource generator type: " + keyValuePair.getFirst())
                    : DataResult.success(keyValuePair.mapFirst(CommonRegisters.RESOURCEGENERATORS::get)));
        }

        @Override
        public <T> DataResult<T> encode(Codec<? extends ResourceGenerator> input, DynamicOps<T> ops, T prefix) {
            ResourceLocation key = CommonRegisters.RESOURCEGENERATORS.inverse().get(input);
            if (key == null)
            {
                return DataResult.error(() -> "Unregistered dynamic resource generator type: " + input);
            }
            T toMerge = ops.createString(key.toString());
            return ops.mergeToPrimitive(prefix, toMerge);
        }
    }).dispatch(ResourceGenerator::codec, Function.identity());

    Codec<ResourceGenerator> PERSISTENT_CACHE_CODEC = CacheMetaCodec.of(CODEC, List.of(
        CacheMetaCodec.SingleCacheType.of(new DataConsumer<>() {
            @Override
            public @NonNull <T> DataResult<T> encode(DynamicOps<T> ops, ResourceCachingData data, ResourceGenerator object) {
                return object.persistentCacheData(ops, data.location(), data.context());
            }
        }, PERSISTENT_CACHE_KEY, ResourceCachingData.class)
    ));

    /**
     * Registers a new resource generator type.
     * @param rl The resource location to register the generator under; becomes the {@code "type"} field in JSON.
     * @param reader The codec used to deserialize the generator from JSON.
     */
    static void register(ResourceLocation rl, Codec<? extends ResourceGenerator> reader) {
        CommonRegisters.RESOURCEGENERATORS.put(rl, reader);
    }

    /**
     * @return A codec that can serialize this resource generator.
     */
    Codec<? extends ResourceGenerator> codec();

    @Override
    @Nullable
    @ApiStatus.NonExtendable
    default String createCacheKey(ResourceLocation outRl, ResourceGenerationContext context) {
        CacheMetaJsonOps ops = new CacheMetaJsonOps();
        ops.putData(ResourceCachingData.class, new ResourceCachingData(outRl, context));
        DataResult<JsonElement> result = PERSISTENT_CACHE_CODEC.encodeStart(ops, this);
        JsonElement element = result.result().orElse(null);
        if (element != null) {
            return DynamicAssetGenerator.GSON_FLAT.toJson(element);
        }
        return PathAwareInputStreamSource.super.createCacheKey(outRl, context);
    }


    /**
     * Create a key that can be <em>uniquely</em> used to identify the resource this generator will generate. Note that
     * this is used for caching across reloads, and so should incorporate any resources that may be used to generate the
     * resource. If this is not possible, return null.
     * @param ops DynamicOps to encode the unique key with.
     * @param location the resource location that will be generated
     * @param context the context that the resource will be generated in. Resources can safely be accessed in this context
     * @return a key that can be used to uniquely identify the resource, or null if this is not possible
     */
    @NonNull
    @ApiStatus.Experimental
    default <T> DataResult<T> persistentCacheData(DynamicOps<T> ops, ResourceLocation location, ResourceGenerationContext context) {
        return DataResult.error(() -> "Resource generators must be made explicitly cacheable by implementing the relevant API");
    }

}
