/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.cache;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import org.jetbrains.annotations.NotNull;

public class CacheMetaCodec<A,D> implements Codec<A> {
    private final Codec<A> wrapped;
    private final DataConsumer<D,A> dataConsumer;
    private final String cacheKey;
    private final Class<? super D> dataClass;

    public CacheMetaCodec(Codec<A> wrapped, DataConsumer<D, A> dataConsumer, String cacheKey, Class<D> dataClass) {
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
        DataResult<T> data = wrapped.encode(input, ops, prefix);
        if (ops instanceof ICacheMetaDynamicOps<T> cacheOps) {
            return data.flatMap(t -> {
                D opsData = cacheOps.getData(dataClass);
                if (opsData != null) {
                    DataResult<T> metadata = dataConsumer.encode(ops, opsData, input);
                    if (metadata.result().isEmpty()) {
                        return DataResult.error("Failed to encode metadata: " + metadata.error().get().message());
                    }
                    if (wrapped instanceof MapCodec.MapCodecCodec<A>) {
                        return ops.mergeToMap(t, ops.createString(this.cacheKey), metadata.result().get());
                    } else {
                        var outBuilder = ops.mapBuilder();
                        outBuilder.add("value", t);
                        outBuilder.add(this.cacheKey, metadata);
                        return outBuilder.build(ops.empty());
                    }
                } else {
                    return DataResult.success(t);
                }
            });
        }
        return data;
    }

    public interface DataConsumer<D, A> {
        @NotNull <T> DataResult<T> encode(DynamicOps<T> ops, D data, A object);
    }
}
