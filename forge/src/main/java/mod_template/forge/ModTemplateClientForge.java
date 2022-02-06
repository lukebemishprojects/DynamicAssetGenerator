package mod_template.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import mod_template.ModTemplate;
import mod_template.ModTemplateClient;

@Mod.EventBusSubscriber(modid = ModTemplate.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModTemplateClientForge {
    public static void init(final FMLClientSetupEvent event) {
        ModTemplateClient.init();
    }
}
