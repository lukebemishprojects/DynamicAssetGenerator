package io.github.lukebemish.dynamic_asset_generator.impl;

import io.github.lukebemish.dynamic_asset_generator.api.IResourceGenerator;
import io.github.lukebemish.dynamic_asset_generator.api.client.DynamicTextureSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DynamicAssetGenerator {
    public static final String MOD_ID = "dynamic_asset_generator";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final ResourceLocation EMPTY_TEXTURE = new ResourceLocation(MOD_ID, "textures/empty.png");
    public static final String SERVER_PACK = DynamicAssetGenerator.MOD_ID+":"+ PackType.SERVER_DATA.getDirectory();
    public static final String CLIENT_PACK = DynamicAssetGenerator.MOD_ID+":"+PackType.CLIENT_RESOURCES.getDirectory();

    private static ModConfig configs;

    public static ModConfig getConfig() {
        if (configs == null) {
            configs = ModConfig.get();
        }
        return configs;
    }

    public static void init() {
        IResourceGenerator.register(new ResourceLocation(DynamicAssetGenerator.MOD_ID,"texture_generator"), DynamicTextureSource.CODEC);
    }
}
