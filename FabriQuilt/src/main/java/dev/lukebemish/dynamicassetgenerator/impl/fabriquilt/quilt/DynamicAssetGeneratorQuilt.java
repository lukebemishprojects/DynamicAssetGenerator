/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.quilt;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.FabriQuiltShared;
import net.minecraft.server.packs.PackType;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

@SuppressWarnings("unused")
public class DynamicAssetGeneratorQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer container) {
        DynamicAssetGenerator.init();
        FabriQuiltShared.registerForType(PackType.SERVER_DATA);
    }
}
