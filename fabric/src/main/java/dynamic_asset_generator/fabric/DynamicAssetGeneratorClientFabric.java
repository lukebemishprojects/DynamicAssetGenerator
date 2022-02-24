package dynamic_asset_generator.fabric;

import dynamic_asset_generator.DynamicAssetGenerator;
import dynamic_asset_generator.DynamicAssetGeneratorClient;
import dynamic_asset_generator.client.DynAssetGenClientPlanner;
import dynamic_asset_generator.client.api.PaletteExtractor;
import net.devtech.arrp.api.RRPCallback;
import net.devtech.arrp.api.RuntimeResourcePack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class DynamicAssetGeneratorClientFabric implements ClientModInitializer {
    public static RuntimeResourcePack RESOURCE_PACK;
    @Override
    public void onInitializeClient() {
        RRPCallback.AFTER_VANILLA.register(a -> {
            PaletteExtractor.refresh();
            RESOURCE_PACK = RuntimeResourcePack.create(DynamicAssetGenerator.CLIENT_PACK);
            Map<ResourceLocation, Supplier<InputStream>> map = DynAssetGenClientPlanner.getResources();
            for (ResourceLocation rl : map.keySet()) {
                InputStream stream = map.get(rl).get();
                if (stream != null) {
                    try {
                        RESOURCE_PACK.addAsset(rl, stream.readAllBytes());
                    } catch (IOException ignored) {}
                }
            }
            a.add(RESOURCE_PACK);
        });
        DynamicAssetGeneratorClient.init();
    }
}
