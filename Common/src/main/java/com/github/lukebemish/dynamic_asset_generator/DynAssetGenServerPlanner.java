package com.github.lukebemish.dynamic_asset_generator;

import com.github.lukebemish.dynamic_asset_generator.api.ResettingSupplier;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DynAssetGenServerPlanner {
    private static Map<ResourceLocation, Supplier<InputStream>> data = new HashMap<>();
    private static final List<Supplier<Map<ResourceLocation,Supplier<InputStream>>>> unresolved = new ArrayList<>();

    public static Map<ResourceLocation, Supplier<InputStream>> getResources() {
        var outMap = new HashMap<>(data);
        for (Supplier<Map<ResourceLocation,Supplier<InputStream>>> l : unresolved) {
            if (l instanceof ResettingSupplier r) {
                r.reset();
            }
            outMap.putAll(l.get());
        }
        for (ResourceLocation rl : outMap.keySet()) {
            Supplier<InputStream> d = outMap.get(rl);
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
                    outMap.put(rl, supplier);
                }
            }
        }
        return outMap;
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

    public static void planLoaders(Supplier<Map<ResourceLocation, Supplier<InputStream>>> suppliers) {
        unresolved.add(()->{
            var map = suppliers.get();
            Map<ResourceLocation,Supplier<InputStream>> out = new HashMap<>();
            for (ResourceLocation location : map.keySet()) {
                if(DynamicAssetGenerator.getConfig().cacheData) {
                    try {
                        if (!Files.exists(ModConfig.DATA_CACHE_FOLDER)) Files.createDirectories(ModConfig.DATA_CACHE_FOLDER);
                        if (Files.exists(ModConfig.DATA_CACHE_FOLDER.resolve(location.getNamespace()).resolve(location.getPath()))) {
                            out.put(location, () -> {
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
                            continue;
                        }
                    } catch (IOException e) {
                        DynamicAssetGenerator.LOGGER.error("Could not cache data...",e);
                    }
                }
                out.put(location,map.get(location));
            }
            return out;
        });
    }
}
