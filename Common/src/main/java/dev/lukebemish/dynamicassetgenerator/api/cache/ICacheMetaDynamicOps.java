/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.cache;

import com.mojang.serialization.DynamicOps;
import org.jetbrains.annotations.Nullable;

public interface ICacheMetaDynamicOps<T> extends DynamicOps<T> {
    @Nullable <D> D getData(Class<? super D> clazz);

    <D> void putData(Class<? super D> clazz, @Nullable D data);
}
