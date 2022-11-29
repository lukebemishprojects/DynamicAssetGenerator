/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.forge;

import dev.lukebemish.dynamicassetgenerator.impl.DynAssetGenServerDataPack;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.client.DynAssetGenClientResourcePack;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;

public class EventHandler {
    public static void addResourcePack(AddPackFindersEvent event) {
        DynamicAssetGenerator.LOGGER.info("Attempting pack insertion...");
        PackType type = event.getPackType();
        if (type == PackType.CLIENT_RESOURCES) {
            event.addRepositorySource((packConsumer, constructor) -> {
                Pack pack = Pack.create(DynamicAssetGenerator.CLIENT_PACK, true, DynAssetGenClientResourcePack::new, constructor, Pack.Position.TOP, PackSource.DEFAULT);
                if (pack != null) {
                    packConsumer.accept(pack);
                } else {
                    DynamicAssetGenerator.LOGGER.error("Couldn't inject client assets!");
                }
            });
        } else if (type == PackType.SERVER_DATA) {
            event.addRepositorySource((packConsumer, constructor) -> {
                Pack pack = Pack.create(DynamicAssetGenerator.SERVER_PACK, true, DynAssetGenServerDataPack::new, constructor, Pack.Position.TOP, PackSource.DEFAULT);
                if (pack != null) {
                    packConsumer.accept(pack);
                } else {
                    DynamicAssetGenerator.LOGGER.error("Couldn't inject server data!");
                }
            });
        }
    }
}
