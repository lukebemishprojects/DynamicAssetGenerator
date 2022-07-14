package io.github.lukebemish.dynamic_asset_generator.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.impl.platform.Services;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public record ModConfig(boolean cacheAssets, boolean cacheData) {
    public static final Codec<ModConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("cache_assets").forGetter(ModConfig::cacheAssets),
            Codec.BOOL.fieldOf("cache_data").forGetter(ModConfig::cacheData)
    ).apply(instance, ModConfig::new));
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    public static final Path FULL_PATH = Services.PLATFORM.getConfigFolder().resolve(DynamicAssetGenerator.MOD_ID+".json");
    public static final Path ASSET_CACHE_FOLDER = Services.PLATFORM.getModDataFolder().resolve(DynamicAssetGenerator.MOD_ID+"/asset_cache");
    public static final Path DATA_CACHE_FOLDER = Services.PLATFORM.getModDataFolder().resolve(DynamicAssetGenerator.MOD_ID+"/data_cache");

    private static ModConfig load() {
        ModConfig config = getDefault();
        try {
            checkExistence();
            JsonObject json = GSON.fromJson(Files.newBufferedReader(FULL_PATH), JsonObject.class);
            var either = CODEC.parse(JsonOps.INSTANCE, json).get();
            var left = either.left();
            if (left.isPresent()) {
                config = left.get();
            } else {
                DynamicAssetGenerator.LOGGER.error("Config is in the wrong format! An attempt to load with this config would crash. Using default config instead...");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    public static ModConfig get() {
        return load();
    }

    private static void checkExistence() throws IOException {
        if (!Files.exists(FULL_PATH.getParent())) Files.createDirectories(FULL_PATH.getParent());
        if (!Files.exists(FULL_PATH)) {
            Files.createFile(FULL_PATH);
            try {
                ModConfig config = getDefault();
                Writer writer = Files.newBufferedWriter(FULL_PATH);
                JsonElement json = CODEC.encodeStart(JsonOps.INSTANCE, config).getOrThrow(false, e -> {});
                GSON.toJson(json, writer);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static ModConfig getDefault() {
        return new ModConfig(false, false);
    }
}
