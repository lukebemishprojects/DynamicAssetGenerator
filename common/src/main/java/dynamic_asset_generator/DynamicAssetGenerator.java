package dynamic_asset_generator;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DynamicAssetGenerator {
    public static final String MOD_ID = "dynamic_asset_generator";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final ResourceLocation EMPTY_TEXTURE = new ResourceLocation(MOD_ID, "textures/empty.png");
    public static final String SERVER_PACK = DynamicAssetGenerator.MOD_ID+":data";
    public static final String CLIENT_PACK = DynamicAssetGenerator.MOD_ID+":client";

    private static ModConfig configs;

    public static ModConfig getConfig() {
        if (configs == null) {
            configs = ModConfig.get();
        }
        return configs;
    }
}
