/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.platform;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.platform.services.Platform;
import dev.lukebemish.dynamicassetgenerator.impl.platform.services.ResourceDegrouper;

import java.util.ServiceLoader;

public class Services {
    public static final Platform PLATFORM = load(Platform.class);
    public static final ResourceDegrouper DEGROUPER = load(ResourceDegrouper.class);

    public static <T> T load(Class<T> clazz) {

        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        DynamicAssetGenerator.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
