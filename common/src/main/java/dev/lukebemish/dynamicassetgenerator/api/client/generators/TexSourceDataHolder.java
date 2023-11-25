/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Data available during the generation of a texture within a {@link TexSource}.
 */
public class TexSourceDataHolder {
    /**
     * The token used to store and retrieve the logger from a {@link TexSourceDataHolder}.
     */
    public static final Token<Logger> LOGGER_TOKEN = new Token<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(TexSource.class);

    /**
     * Creates a new {@link TexSourceDataHolder} with the default logger and no other data.
     */
    public TexSourceDataHolder() {
        put(LOGGER_TOKEN, LOGGER);
    }

    /**
     * Creates a new {@link TexSourceDataHolder} with the data from the provided holder.
     * @param old the holder to copy data from
     */
    public TexSourceDataHolder(TexSourceDataHolder old) {
        dataMap.putAll(old.dataMap);
    }

    private final Map<Token<?>, Object> dataMap = new IdentityHashMap<>();

    /**
     * Adds the provided data to this holder, with the provided class as a key.
     * @param token acts as a key for the data
     * @param data the data to store
     * @param <T> the type of the data
     */
    public <T> void put(Token<T> token, T data) {
        dataMap.put(token, data);
    }

    /**
     * Gets the data stored in this holder, with the provided class as a key.
     * @param token acts as a key for the data
     * @return the data stored, or null if none is stored
     * @param <T> the type of the data
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(Token<T> token) {
        return (T) dataMap.get(token);
    }

    /**
     * @return the logger stored, or the default logger if none is stored
     */
    public Logger getLogger() {
        Logger logger = get(LOGGER_TOKEN);
        return logger == null ? LOGGER : logger;
    }

    /**
     * A token used to store and retrieve data from a {@link TexSourceDataHolder}. Compared based on identity.
     * @param <T> the type of the data
     */
    @SuppressWarnings("unused")
    public static final class Token<T> {}
}
