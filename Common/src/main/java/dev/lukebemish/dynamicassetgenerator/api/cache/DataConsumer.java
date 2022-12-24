/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.cache;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import org.jetbrains.annotations.NotNull;

public interface DataConsumer<D, A> {
    @NotNull <T> DataResult<T> encode(DynamicOps<T> ops, D data, A object);
}
