package palette_extractor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class PaletteExtractorClient {
    public static void init() {
        //testing
        /*
        PaletteExtractorPlanner.planPaletteCombinedImage(new ResourceLocation("minecraft","textures/item/gold_ingot.png"),
                new PlannedPaletteCombinedImage(new ResourceLocation("minecraft","textures/item/redstone.png"), new ResourceLocation("palette_extractor","textures/empty.png"), new ResourceLocation("minecraft","textures/item/iron_ingot.png"), false, true));
        */
    }
}
