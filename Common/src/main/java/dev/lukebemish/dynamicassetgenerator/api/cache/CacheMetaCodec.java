/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.cache;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class CacheMetaCodec<A, D> implements Codec<A> {
    private final Codec<A> wrapped;
    private final DataConsumer<D,A> dataConsumer;
    private final String cacheKey;
    private final Class<? super D> dataClass;

    public CacheMetaCodec(Codec<A> wrapped, DataConsumer<D, A> dataConsumer, String cacheKey, Class<? super D> dataClass) {
        this.wrapped = wrapped;
        this.dataConsumer = dataConsumer;
        this.cacheKey = cacheKey;
        this.dataClass = dataClass;
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        return wrapped.decode(ops, input);
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        DataResult<T> result = wrapped.encode(input, ops, prefix);
        if (ops instanceof CacheMetaDynamicOps<T> cacheOps) {
            D opsData = cacheOps.getData(dataClass);
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
        }
        return result;
    }

    public static <A,D> Codec<A> of(Codec<A> wrapped, DataConsumer<D, A> dataConsumer, String cacheKey, Class<D> dataClass) {
        return new CacheMetaCodec<>(wrapped, dataConsumer, cacheKey, dataClass);
    }
}
