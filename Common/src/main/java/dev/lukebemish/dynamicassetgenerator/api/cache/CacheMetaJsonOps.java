/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.cache;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.jetbrains.annotations.Nullable;

public class CacheMetaJsonOps<D> extends JsonOps implements ICacheMetaDynamicOps<JsonElement> {
    final D data;
    final Class<? super D> clazz;
    public CacheMetaJsonOps(D data, Class<? super D> clazz) {
        super(false);
        this.data = data;
        this.clazz = clazz;
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <D1> D1 getData(Class<? super D1> clazz) {
        if (clazz.isAssignableFrom(this.clazz)) {
            return (D1) data;
        }
        return null;
    }
}
