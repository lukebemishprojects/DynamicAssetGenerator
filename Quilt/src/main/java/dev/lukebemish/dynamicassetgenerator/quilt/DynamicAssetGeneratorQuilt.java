/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.quilt;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.GeneratedPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
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
        ResourceLoader.get(type).getRegisterTopResourcePackEvent().register(context ->
                DynamicAssetGenerator.caches.forEach((location, info) -> {
                    if (info.cache().getPackType() == type && info.position() == Pack.Position.TOP) {
                        context.addResourcePack(new GeneratedPackResources(info.cache()));
                    }
                })
        );
        ResourceLoader.get(type).getRegisterDefaultResourcePackEvent().register(context ->
                DynamicAssetGenerator.caches.forEach((location, info) -> {
                    if (info.cache().getPackType() == type && info.position() == Pack.Position.BOTTOM) {
                        context.addResourcePack(new GeneratedPackResources(info.cache()));
                    }
                })
        );
    }
}
