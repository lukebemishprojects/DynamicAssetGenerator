package io.github.lukebemish.dynamic_asset_generator.impl;

import io.github.lukebemish.dynamic_asset_generator.api.IResourceGenerator;
import io.github.lukebemish.dynamic_asset_generator.api.generators.DummyGenerator;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DynamicAssetGenerator {
    public static final String MOD_ID = "dynamic_asset_generator";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final String SERVER_PACK = DynamicAssetGenerator.MOD_ID+":"+ PackType.SERVER_DATA.getDirectory();
    public static final String CLIENT_PACK = DynamicAssetGenerator.MOD_ID+":"+PackType.CLIENT_RESOURCES.getDirectory();

    public static final PackMetadataSection SERVER_PACK_METADATA =
            new PackMetadataSection(Component.literal("Dynamic Asset Generator: Generated Data"),
                    PackType.SERVER_DATA.getVersion(SharedConstants.getCurrentVersion()));
    public static final PackMetadataSection CLIENT_PACK_METADATA =
            new PackMetadataSection(Component.literal("Dynamic Asset Generator: Generated Assets"),
                    PackType.CLIENT_RESOURCES.getVersion(SharedConstants.getCurrentVersion()));

    private static ModConfig configs;

    public static ModConfig getConfig() {
        if (configs == null) {
            configs = ModConfig.get();
        }
        return configs;
    }

    public static void init() {
        IResourceGenerator.register(new ResourceLocation(MOD_ID,"dummy"), DummyGenerator.CODEC);
    }
}
