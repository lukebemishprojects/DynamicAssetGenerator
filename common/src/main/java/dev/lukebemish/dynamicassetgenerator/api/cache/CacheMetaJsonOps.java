/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.cache;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link CacheMetaDynamicOps} that uses {@link JsonElement} as its underlying type.
 */
public class CacheMetaJsonOps extends JsonOps implements CacheMetaDynamicOps<JsonElement> {

    private final Map<Class<?>,Object> map = new HashMap<>();
    public CacheMetaJsonOps() {
        super(false);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <D> D getData(Class<? super D> clazz) {
        return (D) map.get(clazz);
    }

    @Override
    public <D> void putData(Class<? super D> clazz, @Nullable D data) {
        map.put(clazz, data);
    }
}
