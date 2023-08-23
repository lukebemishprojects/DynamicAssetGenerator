/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.cache;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.List;

/**
 * A {@link Codec} that adds any metadata provided by a {@link CacheMetaDynamicOps} to the encoded value while encoding.
 * @param <A> the type encoded from and decoded to.
 */
public class CacheMetaCodec<A> implements Codec<A> {
    private final Codec<A> wrapped;
    private final List<SingleCacheType<?,A>> dataConsumers;

    private CacheMetaCodec(Codec<A> wrapped, List<SingleCacheType<?,A>> dataConsumers) {
        this.wrapped = wrapped;
        this.dataConsumers = dataConsumers;
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        return wrapped.decode(ops, input);
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        DataResult<T> result = wrapped.encode(input, ops, prefix);
        if (ops instanceof CacheMetaDynamicOps<T> cacheOps) {
            for (SingleCacheType<?,A> dataConsumer : dataConsumers) {
                result = dataConsumer.encode(input, cacheOps, result);
            }
        }
        return result;
    }

    /**
     * Creates a new codec with the provided behaviour, based on an existing codec.
     * @param wrapped the codec to wrap; will be used directly for decoding, and delegated to for encoding
     * @param dataConsumers the metadata to process while encoding
     * @return a new codec with the provided behaviour
     * @param <A> the type encoded from and decoded to
     */
    public static <A> Codec<A> of(Codec<A> wrapped, List<SingleCacheType<?,A>> dataConsumers) {
        return new CacheMetaCodec<>(wrapped, dataConsumers);
    }

    /**
     * A single type of metadata to process while encoding.
     * @param <D> the type of metadata to process while encoding
     * @param <A> the type encoded from and decoded to
     */
    public static final class SingleCacheType<D, A> {
        private final DataConsumer<D,A> dataConsumer;
        private final String cacheKey;
        private final Class<? super D> dataClass;

        private SingleCacheType(DataConsumer<D, A> dataConsumer, String cacheKey, Class<? super D> dataClass) {
            this.dataConsumer = dataConsumer;
            this.cacheKey = cacheKey;
            this.dataClass = dataClass;
        }

        /**
         * Creates a new {@link SingleCacheType} with the provided behaviour, multiple of which can be accumulated onto
         * a single caching codec.
         * @param dataConsumer provides encodable data given the context of the encoding and the object to encode
         * @param cacheKey the key to use for the metadata in the encoded value
         * @param dataClass the class of the metadata to process while encoding
         * @return a new {@link SingleCacheType} with the provided behaviour
         * @param <D> the type of metadata to process while encoding
         * @param <A> the type encoded from and decoded to
         */
        public static <D,A> SingleCacheType<D,A> of(DataConsumer<D, A> dataConsumer, String cacheKey, Class<? super D> dataClass) {
            return new SingleCacheType<>(dataConsumer, cacheKey, dataClass);
        }

        public <T> DataResult<T> encode(A input, CacheMetaDynamicOps<T> ops, DataResult<T> result) {
            D opsData = ops.getData(dataClass);
            if (opsData != null) {
                DataResult<T> metadata = dataConsumer.encode(ops, opsData, input);
                if (metadata.result().isEmpty()) {
                    return metadata;
                }
                return result.flatMap(t -> {
                    if (ops.getMap(t).result().isPresent()) {
                        return ops.mergeToMap(t, ops.createString(cacheKey), metadata.result().get());
                    } else {
                        var builder = ops.mapBuilder();
                        builder.add(ops.createString(cacheKey), metadata.result().get());
                        builder.add("value", t);
                        return builder.build(ops.empty());
                    }
                });
            }
            return result;
        }
    }
}
