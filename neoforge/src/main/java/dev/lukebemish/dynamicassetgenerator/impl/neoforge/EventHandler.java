/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.neoforge;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.GeneratedPackResources;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.util.List;

public class EventHandler {
    public static void addResourcePack(AddPackFindersEvent event) {
        DynamicAssetGenerator.LOGGER.info("Attempting pack insertion...");
        PackType type = event.getPackType();
        DynamicAssetGenerator.CACHES.forEach((location, info) -> {
            if (info.cache().getPackType() == type) {
                event.addRepositorySource(consumer -> {
                    var metadata = DynamicAssetGenerator.makeMetadata(info.cache());
                    var packInfo = new Pack.Info(
                        metadata.description(),
                        PackCompatibility.COMPATIBLE,
                        FeatureFlagSet.of(),
                        List.of(),
                        true
                    );
                    Pack pack = Pack.create(
                        DynamicAssetGenerator.MOD_ID + '/' + info.cache().getName(),
                        Component.literal(DynamicAssetGenerator.MOD_ID+'/'+ info.cache().getName()),
                        true,
                        new GeneratedPackResources.GeneratedResourcesSupplier(info.cache()),
                        packInfo,
                        info.position(),
                        true,
                        PackSource.DEFAULT
                    );
                    consumer.accept(pack);
                });
            }
        });
    }
}
