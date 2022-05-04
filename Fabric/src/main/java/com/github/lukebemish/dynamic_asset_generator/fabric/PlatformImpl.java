package com.github.lukebemish.dynamic_asset_generator.fabric;

import com.github.lukebemish.dynamic_asset_generator.platform.services.IPlatform;
import com.google.auto.service.AutoService;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

@AutoService(IPlatform.class)
public class PlatformImpl implements IPlatform {
    public Path getConfigFolder() {
        return FabricLoader.getInstance().getConfigDir();
    }
    public boolean isDev() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
