package com.github.lukebemish.dynamic_asset_generator.quilt;

import com.github.lukebemish.dynamic_asset_generator.client.api.PaletteExtractor;
import com.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import com.github.lukebemish.dynamic_asset_generator.DynamicAssetGeneratorClient;
import com.github.lukebemish.dynamic_asset_generator.client.DynAssetGenClientPlanner;
import net.devtech.arrp.api.RRPCallback;
import net.devtech.arrp.api.RuntimeResourcePack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

public class DynamicAssetGeneratorClientQuilt implements ClientModInitializer {
    public static RuntimeResourcePack RESOURCE_PACK;
    @Override
    public void onInitializeClient(ModContainer container) {
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
