package palette_extractor.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import palette_extractor.PaletteExtractorClient;

@Environment(EnvType.CLIENT)
public class PaletteExtractorClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PaletteExtractorClient.init();
    }
}
