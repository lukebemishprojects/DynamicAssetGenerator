/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.forge;

import com.google.auto.service.AutoService;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.platform.services.Platform;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

@AutoService(Platform.class)
public class PlatformImpl implements Platform {
    private static final String MOD_VERSION = ModList.get().getModFileById(DynamicAssetGenerator.MOD_ID).versionString();

    public Path getConfigFolder() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Path getModDataFolder() {
        return FMLPaths.GAMEDIR.get().resolve("mod_data/"+ DynamicAssetGenerator.MOD_ID);
    }

    @Override
    public String getModVersion() {
        return MOD_VERSION;
    }

}
