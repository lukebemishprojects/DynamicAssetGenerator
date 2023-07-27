/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.forge;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.client.DynamicAssetGeneratorClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(DynamicAssetGenerator.MOD_ID)
public class DynamicAssetGeneratorForge {
    public DynamicAssetGeneratorForge() {
        IEventBus modbus = FMLJavaModLoadingContext.get().getModEventBus();
        DynamicAssetGenerator.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            DynamicAssetGeneratorClient.init();
        }
        modbus.addListener(this::constructModEvent);
        modbus.addListener(EventHandler::addResourcePack);
    }

    private void constructModEvent(FMLConstructModEvent event) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            event.enqueueWork(DynamicAssetGeneratorClient::setup);
        }
    }
}
