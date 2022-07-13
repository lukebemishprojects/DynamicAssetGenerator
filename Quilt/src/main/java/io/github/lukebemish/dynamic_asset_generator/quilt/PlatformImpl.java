package io.github.lukebemish.dynamic_asset_generator.quilt;

import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.platform.services.IPlatform;
import com.google.auto.service.AutoService;
import org.quiltmc.loader.api.QuiltLoader;

import java.nio.file.Path;

@AutoService(IPlatform.class)
public class PlatformImpl implements IPlatform {
    public Path getConfigFolder() {
        return QuiltLoader.getConfigDir();
    }

    @Override
    public Path getModDataFolder() {
        return QuiltLoader.getGameDir().resolve("mod_data"+ DynamicAssetGenerator.MOD_ID);
    }

    public boolean isDev() {
        return QuiltLoader.isDevelopmentEnvironment();
    }
}
