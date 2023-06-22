/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.quilt;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.GeneratedPackResources;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;

@SuppressWarnings("unused")
public class DynamicAssetGeneratorQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer container) {
        DynamicAssetGenerator.init();
        registerForType(PackType.SERVER_DATA);
    }

    static void registerForType(PackType type) {
        // Can't use the "default" and "top" events as they compute namespaces too early
        ResourceLoader.get(type).registerResourcePackProfileProvider(consumer ->
                DynamicAssetGenerator.CACHES.forEach(((location, info) -> {
                    if (info.cache().getPackType() == type) {
                        var metadata = DynamicAssetGenerator.fromCache(info.cache());
                        Pack pack = Pack.create(
                                DynamicAssetGenerator.MOD_ID+':'+info.cache().getName().toString(),
                                Component.literal(info.cache().getName().toString()),
                                true,
                                s -> new GeneratedPackResources(info.cache()),
                                new Pack.Info(metadata.getDescription(),
                                        metadata.getPackFormat(),
                                        FeatureFlagSet.of()),
                                type,
                                info.position(),
                                true,
                                PackSource.DEFAULT
                        );
                        consumer.accept(pack);
                    }
        })));
    }
}
