package com.github.lukebemish.dynamic_asset_generator.client.api;

import com.github.lukebemish.dynamic_asset_generator.client.DynAssetGenClientPlanner;
import com.github.lukebemish.dynamic_asset_generator.client.util.IPalettePlan;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.function.Supplier;

public class DynAssetGeneratorClientAPI {

    /**
     * @param location full location of the planned image
     * @param supplier supplies a plan for image generation.
     */
    public static void planPaletteCombinedImage(ResourceLocation location, Supplier<IPalettePlan> supplier) {
        DynAssetGenClientPlanner.planPaletteCombinedImage(location, supplier);
    }

    /**
     * @param location full location of the planned image
     * @param plan     plan for image generation. See {@link PlannedPaletteCombinedImage PlannedPaletteCombinedImage}
     */
    public static void planPaletteCombinedImage(ResourceLocation location, IPalettePlan plan) {
        planPaletteCombinedImage(location, ()->plan);
    }

    public static void planLoadingStream(ResourceLocation location, Supplier<InputStream> sup) {
        DynAssetGenClientPlanner.planLoadingStream(location, sup);
    }

}
