package dynamic_asset_generator;

import dynamic_asset_generator.api.ResettingSupplier;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DynAssetGenServerPlanner {
    private static Map<ResourceLocation, Supplier<InputStream>> data = new HashMap<>();

    public static Map<ResourceLocation, Supplier<InputStream>> getResources() {
        for (Supplier<InputStream> d : data.values()) {
            if (d instanceof ResettingSupplier) {
                ((ResettingSupplier) d).reset();
            }
        }
        return data;
    }

    public static void planLoadingStream(ResourceLocation location, Supplier<InputStream> sup) {
        data.put(location, sup);
    }
}
