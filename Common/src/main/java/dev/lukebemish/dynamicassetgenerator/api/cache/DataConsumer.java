/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.cache;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a function that can process any metadata necessary for encoding into an encodable form, given the object
 * to encode and the context of encoding.
 * @param <D> the type to be encoded
 * @param <A> the context available to process metadata from
 */
@FunctionalInterface
public interface DataConsumer<D, A> {
    /**
     * Given the provided object to encode and the context of encoding, encodes the metadata into an encodable form.
     * @param ops allows encoding into the proper format
     * @param data the context available to construct metadata
     * @param object the object to encode
     * @return the encoded metadata
     * @param <T> the type to encode into
     */
    @NotNull <T> DataResult<T> encode(DynamicOps<T> ops, D data, A object);
}
