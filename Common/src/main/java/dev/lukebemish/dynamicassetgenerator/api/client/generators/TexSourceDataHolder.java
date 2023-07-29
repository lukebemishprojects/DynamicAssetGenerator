/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Data available during the generation of a texture within a {@link TexSource}.
 */
public class TexSourceDataHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(TexSource.class);

    /**
     * Creates a new {@link TexSourceDataHolder} with the default logger and no other data.
     */
    public TexSourceDataHolder() {
        put(Logger.class, LOGGER);
    }

    /**
     * Creates a new {@link TexSourceDataHolder} with the data from the provided holder.
     * @param old the holder to copy data from
     */
    public TexSourceDataHolder(TexSourceDataHolder old) {
        dataMap.putAll(old.dataMap);
    }

    private final Map<String, Object> dataMap = new HashMap<>();

    /**
     * Adds the provided data to this holder, with the provided class as a key.
     * @param clazz acts as a "key" for the data
     * @param data the data to store
     * @param <T> the type of the data
     */
    public <T> void put(Class<? extends T> clazz, T data) {
        dataMap.put(clazz.descriptorString(), data);
    }

    /**
     * Gets the data stored in this holder, with the provided class as a key.
     * @param clazz acts as a "key" for the data
     * @return the data stored, or null if none is stored
     * @param <T> the type of the data
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(Class<? extends T> clazz) {
        return (T) dataMap.get(clazz.descriptorString());
    }

    /**
     * @return the logger stored, or the default logger if none is stored
     */
    public Logger getLogger() {
        Logger logger = get(Logger.class);
        return logger==null? LOGGER : logger;
    }
}
