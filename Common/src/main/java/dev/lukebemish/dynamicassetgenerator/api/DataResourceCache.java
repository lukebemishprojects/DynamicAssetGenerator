/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.api.sources.TagBakery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.NotNull;

public class DataResourceCache extends ResourceCache {
    private final TagBakery tagBakery = new TagBakery();

    public DataResourceCache(ResourceLocation name) {
        super(name);
        this.planSource(tagBakery);
    }

    @Override
    public boolean shouldCache() {
        return DynamicAssetGenerator.getConfig().cacheData();
    }

    @Override
    public @NotNull PackType getPackType() {
        return PackType.SERVER_DATA;
    }

    @SuppressWarnings("unused")
    public TagBakery tags() {
        return tagBakery;
    }
}
