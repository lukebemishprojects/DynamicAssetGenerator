package io.github.lukebemish.dynamic_asset_generator.fabric;

import io.github.lukebemish.dynamic_asset_generator.DynAssetGenServerPlanner;
import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import net.devtech.arrp.api.RRPCallback;
import net.devtech.arrp.api.RuntimeResourcePack;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

public class DynamicAssetGeneratorFabric implements ModInitializer {
    public static RuntimeResourcePack DATA_PACK;
    @Override
    public void onInitialize() {
        RRPCallback.AFTER_VANILLA.register(a -> {
            DATA_PACK = RuntimeResourcePack.create(DynamicAssetGenerator.SERVER_PACK);
            Map<ResourceLocation, Supplier<InputStream>> map = DynAssetGenServerPlanner.getResources();
            for (ResourceLocation rl : map.keySet()) {
                Supplier<InputStream> stream = map.get(rl);
                if (stream != null) {
                    DATA_PACK.addLazyResource(PackType.SERVER_DATA, rl, (i,r)-> {
                        try (InputStream is = stream.get()) {
                            if (is==null) DynamicAssetGenerator.LOGGER.error("No InputStream supplied for {}; attempting to not die terribly...", rl);
                            return is==null? null : is.readAllBytes();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    });
                }
            }
            a.add(DATA_PACK);
        });
    }
}
