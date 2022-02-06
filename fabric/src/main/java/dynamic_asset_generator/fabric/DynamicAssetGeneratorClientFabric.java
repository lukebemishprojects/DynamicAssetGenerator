package dynamic_asset_generator.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import dynamic_asset_generator.DynamicAssetGeneratorClient;

@Environment(EnvType.CLIENT)
public class DynamicAssetGeneratorClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DynamicAssetGeneratorClient.init();
    }
}
