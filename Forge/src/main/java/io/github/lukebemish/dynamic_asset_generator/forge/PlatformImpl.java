package io.github.lukebemish.dynamic_asset_generator.forge;

import io.github.lukebemish.dynamic_asset_generator.platform.services.IPlatform;
import com.google.auto.service.AutoService;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

@AutoService(IPlatform.class)
public class PlatformImpl implements IPlatform {
    public Path getConfigFolder() {
        return FMLPaths.CONFIGDIR.get();
    }
    public boolean isDev() {
        return !FMLLoader.isProduction();
    }
}
