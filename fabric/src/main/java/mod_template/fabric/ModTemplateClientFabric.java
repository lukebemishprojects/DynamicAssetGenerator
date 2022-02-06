package mod_template.fabric;

import net.fabricmc.api.ClientModInitializer;
import mod_template.ModTemplateClient;

public class ModTemplateClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModTemplateClient.init();
    }
}
