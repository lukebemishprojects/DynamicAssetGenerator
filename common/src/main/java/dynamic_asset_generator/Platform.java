package dynamic_asset_generator;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

public class Platform {
    @ExpectPlatform
    public static Path getConfigFolder() {
        throw new AssertionError();
    }
}
