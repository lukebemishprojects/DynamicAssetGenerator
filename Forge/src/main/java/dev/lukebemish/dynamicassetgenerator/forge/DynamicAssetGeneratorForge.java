/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.forge;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.client.DynamicAssetGeneratorClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DynamicAssetGenerator.MOD_ID)
public class DynamicAssetGeneratorForge {
    public DynamicAssetGeneratorForge() {
        IEventBus modbus = FMLJavaModLoadingContext.get().getModEventBus();
        DynamicAssetGenerator.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> DynamicAssetGeneratorClient::init);
        modbus.addListener(EventHandler::addResourcePack);
    }
}
