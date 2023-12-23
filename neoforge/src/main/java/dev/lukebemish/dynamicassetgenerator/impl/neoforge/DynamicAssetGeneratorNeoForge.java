/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.neoforge;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.client.DynamicAssetGeneratorClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(DynamicAssetGenerator.MOD_ID)
public class DynamicAssetGeneratorNeoForge {
    public DynamicAssetGeneratorNeoForge(IEventBus modBus) {
        DynamicAssetGenerator.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            DynamicAssetGeneratorClient.init();
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(PlatformClientImpl::reloadListenerListener);
        }
        modBus.addListener(EventHandler::addResourcePack);
    }
}
