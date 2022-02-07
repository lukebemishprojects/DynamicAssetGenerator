package dynamic_asset_generator.client;

import dynamic_asset_generator.DynamicAssetGenerator;
import dynamic_asset_generator.api.ResettingSupplier;
import dynamic_asset_generator.client.palette.Palette;
import dynamic_asset_generator.client.util.IPalettePlan;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DynAssetGenClientPlanner {
    private static Map<ResourceLocation, Supplier<IPalettePlan>> plannedPaletteCombinedImages = new HashMap<>();
    private static Map<ResourceLocation, Supplier<InputStream>> miscResources = new HashMap<>();

    public static void planPaletteCombinedImage(ResourceLocation rl, IPalettePlan image) {
        plannedPaletteCombinedImages.put(rl, () -> image);
    }

    public static void planPaletteCombinedImage(ResourceLocation rl, Supplier<IPalettePlan> image) {
        plannedPaletteCombinedImages.put(rl, image);
    }

    public static Map<ResourceLocation, Supplier<InputStream>> getResources() {
        Map<ResourceLocation, Supplier<InputStream>> output = new HashMap<>();
        for (ResourceLocation key : plannedPaletteCombinedImages.keySet()) {
            IPalettePlan planned = plannedPaletteCombinedImages.get(key).get();
            BufferedImage image = Palette.paletteCombinedImage(key, planned);
            if (image != null) {
                Supplier<InputStream> s = () -> {
                    try {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        ImageIO.write(image, "png", os);
                        InputStream is = new ByteArrayInputStream(os.toByteArray());
                        return is;
                    } catch (IOException e) {
                        DynamicAssetGenerator.LOGGER.error("Could not write buffered image to stream: " + key.toString());
                    }
                    return null;
                };
                output.put(key, s);
            }
        }
        for (ResourceLocation key : miscResources.keySet()) {
            if (miscResources.get(key) instanceof ResettingSupplier) {
                ((ResettingSupplier) miscResources.get(key)).reset();
            }
            output.put(key, miscResources.get(key));
        }
        return output;
    }

    public static void planLoadingStream(ResourceLocation location, Supplier<InputStream> sup) {
        miscResources.put(location, sup);
    }
}
