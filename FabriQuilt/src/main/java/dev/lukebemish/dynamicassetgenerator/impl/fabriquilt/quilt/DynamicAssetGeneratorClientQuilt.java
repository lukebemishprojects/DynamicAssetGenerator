/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.quilt;

import dev.lukebemish.dynamicassetgenerator.impl.client.DynamicAssetGeneratorClient;
import dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.FabriQuiltShared;
import net.minecraft.server.packs.PackType;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

@SuppressWarnings("unused")
public class DynamicAssetGeneratorClientQuilt implements ClientModInitializer {
    @Override
    public void onInitializeClient(ModContainer container) {
        DynamicAssetGeneratorClient.init();
        FabriQuiltShared.registerForType(PackType.CLIENT_RESOURCES);
    }
}
