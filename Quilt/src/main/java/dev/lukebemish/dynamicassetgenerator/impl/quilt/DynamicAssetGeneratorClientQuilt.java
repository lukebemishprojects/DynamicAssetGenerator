/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.quilt;

import dev.lukebemish.dynamicassetgenerator.impl.client.DynamicAssetGeneratorClient;
import net.minecraft.server.packs.PackType;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

@SuppressWarnings("unused")
public class DynamicAssetGeneratorClientQuilt implements ClientModInitializer {
    @Override
    public void onInitializeClient(ModContainer container) {
        DynamicAssetGeneratorClient.init();
        DynamicAssetGeneratorQuilt.registerForType(PackType.CLIENT_RESOURCES);
    }
}
