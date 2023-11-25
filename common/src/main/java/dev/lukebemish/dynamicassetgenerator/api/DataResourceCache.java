/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import dev.lukebemish.dynamicassetgenerator.api.sources.TagSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jspecify.annotations.NonNull;

/**
 * A {@link ResourceCache} meant to provide resources for data packs.
 */
public class DataResourceCache extends ResourceCache {
    private final TagSupplier.TagBakery tagBakery = new TagSupplier.TagBakery();

    /**
     * @param name a unique identifier for this cache
     */
    public DataResourceCache(ResourceLocation name) {
        super(name);
        this.planSource(tagBakery);
    }

    @Override
    public @NonNull PackType getPackType() {
        return PackType.SERVER_DATA;
    }

    /**
     * @return a tool to easily add any number of tag entries to this cache
     */
    @SuppressWarnings("unused")
    public TagSupplier.TagBakery tags() {
        return tagBakery;
    }
}
