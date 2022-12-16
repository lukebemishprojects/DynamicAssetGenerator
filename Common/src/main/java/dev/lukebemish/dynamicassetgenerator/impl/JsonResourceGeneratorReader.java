/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.dynamicassetgenerator.api.IPathAwareInputStreamSource;
import dev.lukebemish.dynamicassetgenerator.api.IResourceGenerator;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JsonResourceGeneratorReader implements IPathAwareInputStreamSource {
    private final Map<ResourceLocation, IResourceGenerator> map = new HashMap<>();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    public JsonResourceGeneratorReader(Map<ResourceLocation, String> map) {
        map.forEach((rl, str) -> {
            try {
                IResourceGenerator json = fromJson(str);
                if (json != null && json.getLocations().size() > 0) {
                    json.getLocations().forEach(localRl -> this.map.put(localRl, json));
                }
            } catch (RuntimeException e) {
                DynamicAssetGenerator.LOGGER.error("Could not read json source at {}\n",rl,e);
            }
        } );
    }

    @Nullable
    static IResourceGenerator fromJson(String json) {
        JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
        return IResourceGenerator.CODEC.parse(JsonOps.INSTANCE, jsonObject).getOrThrow(false, s->{});
    }

    @Override
    public IoSupplier<InputStream> get(ResourceLocation outRl, ResourceGenerationContext context) {
        IResourceGenerator json = map.get(outRl);
        if (json!=null)
            return json.get(outRl, context);
        return null;
    }

    @Override
    public @NotNull Set<ResourceLocation> getLocations() {
        return map.keySet();
    }
}
