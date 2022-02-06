package palette_extractor.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import palette_extractor.PaletteExtractor;

@Mod(PaletteExtractor.MOD_ID)
public class PaletteExtractorForge {
    public PaletteExtractorForge() {
        IEventBus modbus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modbus.addListener(PaletteExtractorClientForge::init));
    }
}
