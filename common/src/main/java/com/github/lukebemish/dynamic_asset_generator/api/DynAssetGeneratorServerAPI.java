package com.github.lukebemish.dynamic_asset_generator.api;

import com.github.lukebemish.dynamic_asset_generator.DynAssetGenServerPlanner;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

public class DynAssetGeneratorServerAPI {
    public static void planLoadingStream(ResourceLocation location, Supplier<InputStream> sup) {
        DynAssetGenServerPlanner.planLoadingStream(location, sup);
    }
    public static void planLoaders(Supplier<Map<ResourceLocation,Supplier<InputStream>>> suppliers) {
        DynAssetGenServerPlanner.planLoaders(suppliers);
    }
}
