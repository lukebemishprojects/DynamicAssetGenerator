package dynamic_asset_generator;

import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DynAssetGenServerPlanner {
    private static Map<ResourceLocation, Supplier<InputStream>> data = new HashMap<>();

    public static Map<ResourceLocation, Supplier<InputStream>> getResources() {
        return data;
    }

    public static void planLoadingStream(ResourceLocation location, Supplier<InputStream> sup) {
        data.put(location, sup);
    }
}
