/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client;

import dev.lukebemish.dynamicassetgenerator.api.client.AssetResourceCache;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.JsonResourceGeneratorReader;
import net.minecraft.resources.ResourceLocation;

public class BuiltinAssetResourceCache extends AssetResourceCache {
    public BuiltinAssetResourceCache(ResourceLocation name) {
        super(name);
        this.planSource(() -> new JsonResourceGeneratorReader(context -> JsonResourceGeneratorReader.getSourceJsons(DynamicAssetGenerator.SOURCE_JSON_DIR, context)));
    }
}
