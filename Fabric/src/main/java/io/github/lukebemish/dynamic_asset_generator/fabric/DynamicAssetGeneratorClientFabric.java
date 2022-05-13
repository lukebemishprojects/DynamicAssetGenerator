package io.github.lukebemish.dynamic_asset_generator.fabric;

import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGeneratorClient;
import io.github.lukebemish.dynamic_asset_generator.client.DynAssetGenClientPlanner;
import io.github.lukebemish.dynamic_asset_generator.client.api.PaletteExtractor;
import net.devtech.arrp.api.RRPCallback;
import net.devtech.arrp.api.RuntimeResourcePack;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

public class DynamicAssetGeneratorClientFabric implements ClientModInitializer {
    public static RuntimeResourcePack RESOURCE_PACK;
    @Override
    public void onInitializeClient() {
        RRPCallback.AFTER_VANILLA.register(a -> {
            PaletteExtractor.refresh();
            RESOURCE_PACK = RuntimeResourcePack.create(DynamicAssetGenerator.CLIENT_PACK);
            Map<ResourceLocation, Supplier<InputStream>> map = DynAssetGenClientPlanner.getResources();
            for (ResourceLocation rl : map.keySet()) {
                Supplier<InputStream> stream = map.get(rl);
                if (stream != null) {
                    RESOURCE_PACK.addLazyResource(PackType.CLIENT_RESOURCES, rl, (i,r)-> {
                        try {
                            return stream.get().readAllBytes();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    });
                }
            }
            a.add(RESOURCE_PACK);
        });
        DynamicAssetGeneratorClient.init();
    }
}
