/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.cache;

import com.mojang.serialization.*;

import java.util.stream.Stream;

public class CacheMetaMapCodec<A,D> extends MapCodec<A> {
    private final MapCodec<A> wrapped;
    private final DataConsumer<D,A> dataConsumer;
    private final String cacheKey;
    private final Class<? super D> dataClass;

    protected CacheMetaMapCodec(MapCodec<A> wrapped, DataConsumer<D, A> dataConsumer, String cacheKey, Class<D> dataClass) {
        this.wrapped = wrapped;
        this.dataConsumer = dataConsumer;
        this.cacheKey = cacheKey;
        this.dataClass = dataClass;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.concat(this.wrapped.keys(ops), Stream.of(ops.createString(this.cacheKey)));
    }

    @Override
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
        return wrapped.decode(ops, input);
    }

    @Override
    public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        RecordBuilder<T> data = wrapped.encode(input, ops, prefix);
        if (ops instanceof ICacheMetaDynamicOps<T> cacheOps) {
            D opsData = cacheOps.getData(dataClass);
            if (opsData != null) {
                DataResult<T> metadata = dataConsumer.encode(ops, opsData, input);
                if (metadata.result().isEmpty()) {
                    return data.withErrorsFrom(metadata);
                }
                data.add(this.cacheKey, metadata.result().get());
            }
        }
        return data;
    }

    public static <A,D> Codec<A> of(Codec<A> wrapped, DataConsumer<D, A> dataConsumer, String cacheKey, Class<D> dataClass) {
        if (wrapped instanceof MapCodec.MapCodecCodec<A> mapCodecCodec) {
            return new CacheMetaMapCodec<>(mapCodecCodec.codec(), dataConsumer, cacheKey, dataClass).codec();
        }
        return new CacheMetaMapCodec<>(wrapped.fieldOf("value"), dataConsumer, cacheKey, dataClass).codec();
    }
}
