/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.fabriquilt;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.GeneratedPackResources;
import dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.fabric.FabricPlatform;
import dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.quilt.QuiltPlatform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.flag.FeatureFlagSet;

import java.nio.file.Path;
import java.util.List;

public interface FabriQuiltShared {
    @SuppressWarnings("deprecation")
    static FabriQuiltShared getInstance() {
        if (FabricLoader.getInstance().isModLoaded("quilt_loader")) {
            return QuiltPlatform.INSTANCE;
        } else {
            return FabricPlatform.INSTANCE;
        }
    }

    void packForType(PackType type, RepositorySource source);

    boolean isModLoaded(String id);
    String modVersion(String id);
    Path configDir();
    Path cacheDir();

    static void registerForType(PackType type) {
        getInstance().packForType(type, consumer ->
            DynamicAssetGenerator.CACHES.forEach(((location, info) -> {
                if (info.cache().getPackType() == type) {
                    var metadata = DynamicAssetGenerator.fromCache(info.cache());
                    Pack pack = Pack.create(
                        DynamicAssetGenerator.MOD_ID+'/'+info.cache().getName().toString(),
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

    List<? extends PackResources> unpackPacks(List<? extends PackResources> packs);
}
