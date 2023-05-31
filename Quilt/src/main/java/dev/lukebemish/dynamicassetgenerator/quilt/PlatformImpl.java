/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.quilt;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.platform.services.Platform;
import com.google.auto.service.AutoService;
import org.quiltmc.loader.api.QuiltLoader;

import java.nio.file.Path;

@AutoService(Platform.class)
public class PlatformImpl implements Platform {
    public Path getConfigFolder() {
        return QuiltLoader.getConfigDir();
    }

    @Override
    public Path getModDataFolder() {
        return QuiltLoader.getGameDir().resolve("mod_data/"+ DynamicAssetGenerator.MOD_ID);
    }

}
