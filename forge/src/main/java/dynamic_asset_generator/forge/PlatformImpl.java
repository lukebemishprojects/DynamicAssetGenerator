package dynamic_asset_generator.forge;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class PlatformImpl {
    public static Path getConfigFolder() {
        return FMLPaths.CONFIGDIR.get();
    }
}
