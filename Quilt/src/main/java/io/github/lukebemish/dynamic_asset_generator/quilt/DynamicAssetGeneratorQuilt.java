package io.github.lukebemish.dynamic_asset_generator.quilt;

import io.github.lukebemish.dynamic_asset_generator.DynAssetGenServerPlanner;
import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import net.devtech.arrp.api.RRPCallback;
import net.devtech.arrp.api.RuntimeResourcePack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

public class DynamicAssetGeneratorQuilt implements ModInitializer {
    public static RuntimeResourcePack DATA_PACK;
    @Override
    public void onInitialize(ModContainer container) {
        RRPCallback.AFTER_VANILLA.register(a -> {
            DATA_PACK = RuntimeResourcePack.create(DynamicAssetGenerator.SERVER_PACK);
            Map<ResourceLocation, Supplier<InputStream>> map = DynAssetGenServerPlanner.getResources();
            for (ResourceLocation rl : map.keySet()) {
                Supplier<InputStream> stream = map.get(rl);
                if (stream != null) {
                    DATA_PACK.addLazyResource(PackType.SERVER_DATA, rl, (i,r)-> {
                        try (InputStream is = stream.get()) {
                            if (is==null) DynamicAssetGenerator.LOGGER.error("No InputStream supplied for {}; will likely die terribly...", rl);
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
