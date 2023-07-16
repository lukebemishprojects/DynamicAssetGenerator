/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.cache;

import com.mojang.serialization.DynamicOps;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link DynamicOps} that can hold data of various sorts, accessible by codecs, representing a context the encoding
 * or decoding is occurring in.
 * @param <T> the type decoded from and encoded to
 */
public interface CacheMetaDynamicOps<T> extends DynamicOps<T> {
    /**
     * @return data associated with the provided class, or null if there is none
     */
    @Nullable <D> D getData(Class<? super D> clazz);

    /**
     * Attach the provided data to the provided class key in the metadata-providing context this object represents.
     * @param clazz the class to use as a key
     * @param data the data to attach
     * @param <D> the type of data to attach
     */
    <D> void putData(Class<? super D> clazz, @Nullable D data);
}
