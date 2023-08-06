/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.forge;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.platform.services.Platform;
import com.google.auto.service.AutoService;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

@AutoService(Platform.class)
public class PlatformImpl implements Platform {
    public Path getConfigFolder() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Path getModDataFolder() {
        return FMLPaths.GAMEDIR.get().resolve("mod_data/"+ DynamicAssetGenerator.MOD_ID);
    }

}
