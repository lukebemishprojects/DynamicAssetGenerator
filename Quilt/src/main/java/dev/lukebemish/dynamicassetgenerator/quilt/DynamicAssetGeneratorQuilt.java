/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.quilt;

import dev.lukebemish.dynamicassetgenerator.impl.DynAssetGenServerDataPack;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;

@SuppressWarnings("unused")
public class DynamicAssetGeneratorQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer container) {
        ResourceLoader.get(PackType.SERVER_DATA).registerResourcePackProfileProvider(((infoConsumer, infoFactory) -> {
            Pack pack = Pack.create(DynamicAssetGenerator.SERVER_PACK, true, DynAssetGenServerDataPack::new, infoFactory, Pack.Position.TOP, PackSource.DEFAULT);
            if (pack != null) {
                infoConsumer.accept(pack);
            } else {
                DynamicAssetGenerator.LOGGER.error("Couldn't inject server data!");
            }
        }));
        DynamicAssetGenerator.init();
    }
}
