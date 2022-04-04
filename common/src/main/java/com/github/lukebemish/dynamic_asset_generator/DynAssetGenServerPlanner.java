package com.github.lukebemish.dynamic_asset_generator;

import com.github.lukebemish.dynamic_asset_generator.api.ResettingSupplier;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DynAssetGenServerPlanner {
    private static Map<ResourceLocation, Supplier<InputStream>> data = new HashMap<>();

    public static Map<ResourceLocation, Supplier<InputStream>> getResources() {
        for (ResourceLocation rl : data.keySet()) {
            Supplier<InputStream> d = data.get(rl);
            if (d instanceof ResettingSupplier) {
                ((ResettingSupplier<InputStream>) d).reset();
            }
            if (DynamicAssetGenerator.getConfig().cacheData) {
                if (!(d instanceof WrappedSupplier<InputStream>)) {
                    Supplier<InputStream> supplier = new WrappedSupplier<>(() -> {
                        InputStream stream = d.get();
                        if (stream != null) {
                            try {
                                Path path = ModConfig.DATA_CACHE_FOLDER.resolve(rl.getNamespace()).resolve(rl.getPath());
                                if (!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
                                if (!Files.exists(path)) {
                                    Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING);
                                }
                                return new BufferedInputStream(Files.newInputStream(path));
                            } catch (IOException e) {
                                DynamicAssetGenerator.LOGGER.error("Could not save data...", e);
                            }
                        }
                        return stream;
                    });
                    data.put(rl, supplier);
                }
            }
        }
        return data;
    }

    public static void planLoadingStream(ResourceLocation location, Supplier<InputStream> sup) {
        if(DynamicAssetGenerator.getConfig().cacheData) {
            try {
                if (!Files.exists(ModConfig.DATA_CACHE_FOLDER)) Files.createDirectories(ModConfig.DATA_CACHE_FOLDER);
                if (Files.exists(ModConfig.DATA_CACHE_FOLDER.resolve(location.getNamespace()).resolve(location.getPath()))) {
                    data.put(location, () -> {
                        try {
                            File file = ModConfig.DATA_CACHE_FOLDER.resolve(location.getNamespace()).resolve(location.getPath()).toFile();
                            if (file.isFile()) {
                                return new BufferedInputStream(new FileInputStream(file));
                            }
                        } catch (IOException e) {
                            DynamicAssetGenerator.LOGGER.error("Could not load cached data...",e);
                        }
                        return null;
                    });
                    return;
                }
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Could not cache data...",e);
            }
        }
        data.put(location, sup);
    }
}
