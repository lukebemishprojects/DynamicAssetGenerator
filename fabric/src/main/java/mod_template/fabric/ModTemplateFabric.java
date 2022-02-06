package mod_template.fabric;

import mod_template.ModTemplate;
import net.fabricmc.api.ModInitializer;

public class ModTemplateFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModTemplate.init();
    }
}
