/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TexSourceDataHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ITexSource.class);

    public TexSourceDataHolder() {
        put(Logger.class, LOGGER);
    }

    public TexSourceDataHolder(TexSourceDataHolder old) {
        dataMap.putAll(old.dataMap);
    }

    private final Map<String, Object> dataMap = new HashMap<>();

    public <T> void put(Class<? extends T> clazz, T data) {
        dataMap.put(clazz.descriptorString(), data);
    }

    @Nullable
    public <T> T get(Class<? extends T> clazz) {
        try {
            //noinspection unchecked
            return (T) dataMap.get(clazz.descriptorString());
        } catch (ClassCastException ignored) {
            return null;
        }
    }

    public Logger getLogger() {
        Logger logger = get(Logger.class);
        return logger==null? LOGGER : logger;
    }
}
