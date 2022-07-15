package io.github.lukebemish.dynamic_asset_generator.fabric;

import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.impl.platform.services.IPlatform;
import com.google.auto.service.AutoService;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

@AutoService(IPlatform.class)
public class PlatformImpl implements IPlatform {
    public Path getConfigFolder() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path getModDataFolder() {
        return FabricLoader.getInstance().getGameDir().resolve("mod_data/"+ DynamicAssetGenerator.MOD_ID);
    }

    public boolean isDev() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
