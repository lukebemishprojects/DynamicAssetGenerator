package com.github.lukebemish.dynamic_asset_generator.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class PlatformImpl {
    public static Path getConfigFolder() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
