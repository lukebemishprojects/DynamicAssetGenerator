package palette_extractor.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import palette_extractor.PaletteExtractor;
import palette_extractor.PaletteExtractorClient;

@Mod.EventBusSubscriber(modid = PaletteExtractor.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PaletteExtractorClientForge {
    public static void init(final FMLClientSetupEvent event) {
        PaletteExtractorClient.init();
    }
}
