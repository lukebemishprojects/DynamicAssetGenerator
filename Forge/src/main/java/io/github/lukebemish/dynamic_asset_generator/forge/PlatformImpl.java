package io.github.lukebemish.dynamic_asset_generator.forge;

import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
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

    @Override
    public Path getModDataFolder() {
        return FMLPaths.GAMEDIR.get().resolve("mod_data/"+ DynamicAssetGenerator.MOD_ID);
    }

    public boolean isDev() {
        return !FMLLoader.isProduction();
    }
}
