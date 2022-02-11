package dynamic_asset_generator;

import dynamic_asset_generator.api.ResettingSupplier;
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
                ((ResettingSupplier) d).reset();
            }
            InputStream stream = d.get();
            if (DynamicAssetGenerator.getConfig().cacheData) {
                try {
                    Path path = ModConfig.DATA_CACHE_FOLDER.resolve(rl.getNamespace()).resolve(rl.getPath());
                    if (!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
                    if (!Files.exists(path)) {
                        Files.copy(stream,path, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    DynamicAssetGenerator.LOGGER.error("Could not save data...",e);
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
                                return new FileInputStream(file);
                            }
                        } catch (IOException e) {
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
