/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.fabric;

import dev.lukebemish.dynamicassetgenerator.impl.client.DynamicAssetGeneratorClient;
import dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.FabriQuiltShared;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.server.packs.PackType;

public class DynamicAssetGeneratorClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DynamicAssetGeneratorClient.init();
        FabriQuiltShared.registerForType(PackType.CLIENT_RESOURCES);
    }
}
