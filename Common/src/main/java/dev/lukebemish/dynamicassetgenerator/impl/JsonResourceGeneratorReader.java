/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.dynamicassetgenerator.api.PathAwareInputStreamSource;
import dev.lukebemish.dynamicassetgenerator.api.Resettable;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class JsonResourceGeneratorReader implements PathAwareInputStreamSource, Resettable {
    private volatile Map<ResourceLocation, ResourceGenerator> map = null;
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();

    private final Function<ResourceGenerationContext, Map<ResourceLocation, ResourceGenerator>> mapSupplier;

    public JsonResourceGeneratorReader(Function<ResourceGenerationContext, Map<ResourceLocation, String>> mapSupplier) {
        this.mapSupplier = context -> {
            var map = mapSupplier.apply(context);
            Map<ResourceLocation, ResourceGenerator> outMap = new HashMap<>();
            map.forEach((rl, str) -> {
                try {
                    ResourceGenerator json = fromJson(str);
                    if (json != null && !json.getLocations(context).isEmpty()) {
                        json.getLocations(context).forEach(localRl -> outMap.put(localRl, json));
                    }
                } catch (RuntimeException e) {
                    DynamicAssetGenerator.LOGGER.error("Could not read json source at {}\n",rl,e);
                }
            });
            return outMap;
        };
    }

    @Nullable
    static ResourceGenerator fromJson(String json) {
        JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
        return ResourceGenerator.CODEC.parse(JsonOps.INSTANCE, jsonObject).getOrThrow(false, s->{});
    }

    @Override
    public IoSupplier<InputStream> get(ResourceLocation outRl, ResourceGenerationContext context) {
        setupMap(context);
        ResourceGenerator json = map.get(outRl);
        if (json!=null)
            return json.get(outRl, context);
        return null;
    }

    @Override
    public @Nullable String createCacheKey(ResourceLocation outRl, ResourceGenerationContext context) {
        setupMap(context);
        ResourceGenerator json = map.get(outRl);
        if (json!=null)
            return json.createCacheKey(outRl, context);
        return null;
    }

    private void setupMap(ResourceGenerationContext context) {
        if (map == null) {
            synchronized (this) {
                if (map == null) {
                    map = mapSupplier.apply(context);
                }
            }
        }
    }

    @Override
    public @NotNull Set<ResourceLocation> getLocations(ResourceGenerationContext context) {
        setupMap(context);
        return map.keySet();
    }

    @Override
    public void reset(ResourceGenerationContext context) {
        this.map = null;
    }

    public static Map<ResourceLocation, String> getSourceJsons(String sourceJsonDirectory, ResourceGenerationContext context) {
        HashMap<ResourceLocation, String> rls = new HashMap<>();

        var available = context.getResourceSource().listResources(sourceJsonDirectory, rl -> rl.getPath().endsWith(".json"));

        available.forEach((rl, streamSupplier) -> {
            try (InputStream stream = streamSupplier.get()) {
                byte[] bytes = stream.readAllBytes();
                String json = new String(bytes, StandardCharsets.UTF_8);
                rls.put(rl, json);
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Error reading resource source json: " + rl, e);
            }
        });

        return rls;
    }
}
