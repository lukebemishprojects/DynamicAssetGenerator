package dynamic_asset_generator;

import dynamic_asset_generator.api.PlannedPaletteCombinedImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import dynamic_asset_generator.palette.Palette;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class DynAssetGenPlanner {
    private static Map<ResourceLocation, Supplier<PlannedPaletteCombinedImage>> plannedPaletteCombinedImages = new HashMap<>();
    private static Map<ResourceLocation, Supplier<InputStream>> miscResources = new HashMap<>();

    public static void planPaletteCombinedImage(ResourceLocation rl, PlannedPaletteCombinedImage image) {
        plannedPaletteCombinedImages.put(rl, () -> image);
    }

    public static void planPaletteCombinedImage(ResourceLocation rl, Supplier<PlannedPaletteCombinedImage> image) {
        plannedPaletteCombinedImages.put(rl, image);
    }

    public static Map<ResourceLocation, Supplier<InputStream>> getResources() {
        Map<ResourceLocation, Supplier<InputStream>> output = new HashMap<>();
        for (ResourceLocation key : plannedPaletteCombinedImages.keySet()) {
            PlannedPaletteCombinedImage planned = plannedPaletteCombinedImages.get(key).get();
            BufferedImage image = Palette.paletteCombinedImage(planned);
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
            output.put(key, miscResources.get(key));
        }
        return output;
    }

    public static void planLoadingStream(ResourceLocation location, Supplier<InputStream> sup) {
        miscResources.put(location, sup);
    }
}
