package dynamic_asset_generator.api;

import dynamic_asset_generator.DynAssetGenServerPlanner;
import dynamic_asset_generator.client.DynAssetGenClientPlanner;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.function.Supplier;

public class DynAssetGeneratorServerAPI {
    public static void planLoadingStream(ResourceLocation location, Supplier<InputStream> sup) {
        DynAssetGenServerPlanner.planLoadingStream(location, sup);
    }
}
