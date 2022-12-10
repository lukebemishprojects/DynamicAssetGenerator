/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.forge;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.GeneratedPackResources;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;

public class EventHandler {
    public static void addResourcePack(AddPackFindersEvent event) {
        DynamicAssetGenerator.LOGGER.info("Attempting pack insertion...");
        PackType type = event.getPackType();
        DynamicAssetGenerator.caches.forEach((location, info) -> {
            if (info.cache().getPackType() == type) {
                event.addRepositorySource(consumer -> {
                    Pack pack = Pack.readMetaAndCreate(
                            info.cache().getName().toString(),
                            Component.literal(info.cache().getName().toString()),
                            false,
                            s -> new GeneratedPackResources(info.cache()),
                            type,
                            info.position(),
                            PackSource.BUILT_IN
                    );
                    consumer.accept(pack);
                });
            }
        });
    }
}
