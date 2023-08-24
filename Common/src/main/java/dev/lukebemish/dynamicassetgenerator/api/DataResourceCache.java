/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import dev.lukebemish.dynamicassetgenerator.api.sources.TagBakery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link ResourceCache} meant to provide resources for data packs.
 */
public class DataResourceCache extends ResourceCache {
    private final TagBakery tagBakery = new TagBakery();

    /**
     * @param name a unique identifier for this cache
     */
    public DataResourceCache(ResourceLocation name) {
        super(name);
        this.planSource(tagBakery);
    }

    @Override
    public @NotNull PackType getPackType() {
        return PackType.SERVER_DATA;
    }

    /**
     * @return a tool to easily add any number of tag entries to this cache
     */
    @SuppressWarnings("unused")
    public TagBakery tags() {
        return tagBakery;
    }
}
