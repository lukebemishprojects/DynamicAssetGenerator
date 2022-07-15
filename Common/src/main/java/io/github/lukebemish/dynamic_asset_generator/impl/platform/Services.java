package io.github.lukebemish.dynamic_asset_generator.impl.platform;

import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.impl.platform.services.IPlatform;
import io.github.lukebemish.dynamic_asset_generator.impl.platform.services.IResourceDegrouper;

import java.util.ServiceLoader;

public class Services {
    public static final IPlatform PLATFORM = load(IPlatform.class);
    public static final IResourceDegrouper DEGROUPER = load(IResourceDegrouper.class);

    public static <T> T load(Class<T> clazz) {

        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        DynamicAssetGenerator.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
