package palette_extractor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import palette_extractor.palette.Palette;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class PaletteExtractorPlanner {
    public static Map<ResourceLocation, PlannedPaletteCombinedImage> plannedPaletteCombinedImages = new HashMap<>();

    public static void planPaletteCombinedImage(ResourceLocation rl, PlannedPaletteCombinedImage image) {
        plannedPaletteCombinedImages.put(rl, image);
    }

    public static Map<ResourceLocation, BufferedImage> getImages() {
        Map<ResourceLocation, BufferedImage> output = new HashMap<>();
        for (ResourceLocation key : plannedPaletteCombinedImages.keySet()) {
            PlannedPaletteCombinedImage planned = plannedPaletteCombinedImages.get(key);
            BufferedImage image = Palette.paletteCombinedImage(planned.background(), planned.overlay(), planned.paletted(), planned.includeBackground(), planned.extend());
            output.put(key, image);
        }
        return output;
    }
}
