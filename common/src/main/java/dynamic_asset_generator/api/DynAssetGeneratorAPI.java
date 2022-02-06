package dynamic_asset_generator.api;

import dynamic_asset_generator.DynAssetGenPlanner;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.function.Supplier;

public class DynAssetGeneratorAPI {

    /**
     * @param location full location of the planned image
     * @param supplier supplies a plan for image generation.
     */
    public static void planPaletteCombinedImage(ResourceLocation location, Supplier<PlannedPaletteCombinedImage> supplier) {
        DynAssetGenPlanner.planPaletteCombinedImage(location, supplier);
    }

    /**
     * @param location full location of the planned image
     * @param plan     plan for image generation. See {@link PlannedPaletteCombinedImage PlannedPaletteCombinedImage}
     */
    public static void planPaletteCombinedImage(ResourceLocation location, PlannedPaletteCombinedImage plan) {
        planPaletteCombinedImage(location, ()->plan);
    }

    public static void planLoadingStream(ResourceLocation location, Supplier<InputStream> sup) {
        DynAssetGenPlanner.planLoadingStream(location, sup);
    }

}
