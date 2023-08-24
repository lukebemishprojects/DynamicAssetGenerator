/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceCache;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerator;
import dev.lukebemish.dynamicassetgenerator.api.generators.DummyGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.platform.Services;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DynamicAssetGenerator {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();
    public static final Gson GSON_FLAT = new GsonBuilder().setLenient().create();
    public static final String MOD_ID = "dynamic_asset_generator";
    public static final String SOURCE_JSON_DIR = MOD_ID +"/generators";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static PackMetadataSection fromCache(ResourceCache cache) {
        return new PackMetadataSection(Component.literal("Dynamic Asset Generator: " + cache.getName()),
                SharedConstants.getCurrentVersion().getPackVersion(cache.getPackType()));
    }

    private static ModConfig configs;

    public static ModConfig getConfig() {
        if (configs == null) {
            configs = ModConfig.get();
        }
        return configs;
    }

    public static final boolean TIME_RESOURCES = "true".equals(System.getProperty("dynamicassetgenerator.time_resources"))  || getConfig().timeResources();

    public static void init() {
        ResourceGenerator.register(new ResourceLocation(MOD_ID,"dummy"), DummyGenerator.CODEC);
        if (TIME_RESOURCES) {
            LOGGER.info("Dynamic Asset Generator will time resource generation during this run!");
            try {
                Files.deleteIfExists(Services.PLATFORM.getModDataFolder().resolve("times.log"));
            } catch (IOException e) {
                LOGGER.error("Issue deleting times.log; you might be able to ignore this", e);
            }
        }
        ResourceGenerator.register(new ResourceLocation(MOD_ID,"dummy"), DummyGenerator.CODEC);
        ResourceCache.register(new BuiltinDataResourceCache(new ResourceLocation(MOD_ID, "builtin_data")), Pack.Position.TOP);
    }

    public static Path cache(ResourceLocation cacheKey, boolean keyed) {
        return Services.PLATFORM.getModDataFolder().resolve(keyed ? "keyed_cache" : "cache").resolve(cacheKey.getNamespace()).resolve(cacheKey.getPath());
    }

    public static final Map<ResourceLocation, PackInfo> CACHES = new HashMap<>();

    public static void registerCache(ResourceLocation id, ResourceCache cache, Pack.Position position) {
        CACHES.put(id, new PackInfo(cache, position));
    }

    public record PackInfo(ResourceCache cache, Pack.Position position) {

    }
}
