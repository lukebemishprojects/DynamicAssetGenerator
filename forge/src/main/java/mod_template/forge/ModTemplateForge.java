package mod_template.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import mod_template.ModTemplate;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ModTemplate.MOD_ID)
public class ModTemplateForge {
    public ModTemplateForge() {
        IEventBus modbus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(ModTemplate.MOD_ID, modbus);
        ModTemplate.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modbus.addListener(ModTemplateClientForge::init));
    }
}
