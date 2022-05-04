package com.github.lukebemish.dynamic_asset_generator;

import com.github.lukebemish.dynamic_asset_generator.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModConfig {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    public static final Path CONFIG_PATH = Services.PLATFORM.getConfigFolder();
    public static final String FULL_PATH = CONFIG_PATH.toString() + "/"+ DynamicAssetGenerator.MOD_ID+".json";
    public static final Path ASSET_CACHE_FOLDER = CONFIG_PATH.resolve(DynamicAssetGenerator.MOD_ID+"/asset_cache");
    public static final Path DATA_CACHE_FOLDER = CONFIG_PATH.resolve(DynamicAssetGenerator.MOD_ID+"/data_cache");
    public static final int CURRENT_VERSION = 1;

    @Expose
    public int format = 1;
    @Expose
    public boolean cacheAssets = false;
    @Expose
    public boolean cacheData = false;

    private static ModConfig load() {
        ModConfig config = new ModConfig();
        try {
            checkExistence();
            config = GSON.fromJson(new FileReader(FULL_PATH), ModConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    public static ModConfig get() {
        ModConfig config = load();
        if (config.format != CURRENT_VERSION) {
            DynamicAssetGenerator.LOGGER.error("Config is outdated! An attempt to load with this config would crash. Using default config instead...");
            return new ModConfig();
        }
        return config;
    }

    public static void save(ModConfig config) {
        try {
            checkExistence();
            FileWriter writer = new FileWriter(FULL_PATH);
            GSON.toJson(config, writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void checkExistence() throws IOException {
        if (!Files.exists(CONFIG_PATH)) Files.createDirectories(CONFIG_PATH);
        if (!Files.exists(Paths.get(FULL_PATH))) {
            Files.createFile(Paths.get(FULL_PATH));
            save(new ModConfig());
        }
    }
}
