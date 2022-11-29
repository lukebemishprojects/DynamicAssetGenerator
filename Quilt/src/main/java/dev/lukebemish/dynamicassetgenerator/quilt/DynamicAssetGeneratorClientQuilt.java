/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.quilt;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.client.DynAssetGenClientResourcePack;
import dev.lukebemish.dynamicassetgenerator.impl.client.DynamicAssetGeneratorClient;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;

@SuppressWarnings("unused")
public class DynamicAssetGeneratorClientQuilt implements ClientModInitializer {
    @Override
    public void onInitializeClient(ModContainer container) {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerResourcePackProfileProvider(((infoConsumer, infoFactory) -> {
            Pack pack = Pack.create(DynamicAssetGenerator.CLIENT_PACK, true, DynAssetGenClientResourcePack::new, infoFactory, Pack.Position.TOP, PackSource.DEFAULT);
            if (pack != null) {
                infoConsumer.accept(pack);
            } else {
                DynamicAssetGenerator.LOGGER.error("Couldn't inject client assets!");
            }
        }));
        DynamicAssetGeneratorClient.init();
    }
}
