/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lukebemish.dynamicassetgenerator.api.IResourceGenerator;
import dev.lukebemish.dynamicassetgenerator.api.ResourceCache;
import dev.lukebemish.dynamicassetgenerator.api.generators.DummyGenerator;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class DynamicAssetGenerator {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();
    public static final Gson GSON_FLAT = new GsonBuilder().setLenient().create();
    public static final String MOD_ID = "dynamic_asset_generator";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static PackMetadataSection fromCache(ResourceCache cache) {
        return new PackMetadataSection(Component.literal("Dynamic Asset Generator: " + cache.getName()),
                cache.getPackType().getVersion(SharedConstants.getCurrentVersion()));
    }

    private static ModConfig configs;

    public static ModConfig getConfig() {
        if (configs == null) {
            configs = ModConfig.get();
        }
        return configs;
    }

    public static void init() {
        IResourceGenerator.register(new ResourceLocation(MOD_ID,"dummy"), DummyGenerator.CODEC);
        ResourceCache.register(new BuiltinDataResourceCache(new ResourceLocation(MOD_ID, "builtin_data")), Pack.Position.TOP);
    }

    public static final Map<ResourceLocation, PackInfo> caches = new HashMap<>();

    public static void registerCache(ResourceLocation id, ResourceCache cache, Pack.Position position) {
        caches.put(id, new PackInfo(cache, position));
    }

    public record PackInfo(ResourceCache cache, Pack.Position position) {

    }
}
