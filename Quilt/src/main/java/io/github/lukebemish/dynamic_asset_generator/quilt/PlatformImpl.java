package io.github.lukebemish.dynamic_asset_generator.quilt;

import io.github.lukebemish.dynamic_asset_generator.platform.services.IPlatform;
import com.google.auto.service.AutoService;
import org.quiltmc.loader.api.QuiltLoader;

import java.nio.file.Path;

@AutoService(IPlatform.class)
public class PlatformImpl implements IPlatform {
    public Path getConfigFolder() {
        return QuiltLoader.getConfigDir();
    }
    public boolean isDev() {
        return QuiltLoader.isDevelopmentEnvironment();
    }
}
