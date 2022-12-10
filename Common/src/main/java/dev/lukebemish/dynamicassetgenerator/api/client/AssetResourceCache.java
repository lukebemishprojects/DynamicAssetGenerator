/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client;

import dev.lukebemish.dynamicassetgenerator.api.ResourceCache;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.NotNull;

public class AssetResourceCache extends ResourceCache {
    @SuppressWarnings("unused")
    public static final ResourceLocation EMPTY_TEXTURE = new ResourceLocation(DynamicAssetGenerator.MOD_ID, "textures/empty.png");

    public AssetResourceCache(ResourceLocation name) {
        super(name);
    }

    @Override
    public boolean shouldCache() {
        return DynamicAssetGenerator.getConfig().cacheAssets();
    }

    @Override
    public @NotNull PackType getPackType() {
        return PackType.CLIENT_RESOURCES;
    }
}
