/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.platform;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.platform.services.IPlatform;
import dev.lukebemish.dynamicassetgenerator.impl.platform.services.IResourceDegrouper;

import java.util.ServiceLoader;

public class Services {
    public static final IPlatform PLATFORM = load(IPlatform.class);
    public static final IResourceDegrouper DEGROUPER = load(IResourceDegrouper.class);

    public static <T> T load(Class<T> clazz) {

        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        DynamicAssetGenerator.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
