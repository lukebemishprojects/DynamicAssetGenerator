/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.fabriquilt;

import com.google.auto.service.AutoService;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.platform.services.Platform;

import java.nio.file.Path;

@AutoService(Platform.class)
public class PlatformImpl implements Platform {
    private static final String MOD_VERSION = FabriQuiltShared.getInstance().modVersion(DynamicAssetGenerator.MOD_ID);

    public Path getConfigFolder() {
        return FabriQuiltShared.getInstance().configDir();
    }

    @Override
    public Path getModDataFolder() {
        return FabriQuiltShared.getInstance().cacheDir().resolve(DynamicAssetGenerator.MOD_ID);
    }

    @Override
    public String getModVersion() {
        return MOD_VERSION;
    }

}
