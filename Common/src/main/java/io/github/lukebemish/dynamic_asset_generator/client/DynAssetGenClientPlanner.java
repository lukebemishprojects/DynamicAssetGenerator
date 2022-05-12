package io.github.lukebemish.dynamic_asset_generator.client;

import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.ModConfig;
import io.github.lukebemish.dynamic_asset_generator.WrappedSupplier;
import io.github.lukebemish.dynamic_asset_generator.api.ResettingSupplier;
import io.github.lukebemish.dynamic_asset_generator.client.api.ClientPrePackRepository;
import io.github.lukebemish.dynamic_asset_generator.client.api.json.DynamicTextureJson;
import io.github.lukebemish.dynamic_asset_generator.client.util.IPalettePlan;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
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

public class DynAssetGenClientPlanner {
    private static final Map<ResourceLocation, Supplier<InputStream>> miscResources = new HashMap<>();
    private static final List<Supplier<Map<ResourceLocation,Supplier<InputStream>>>> unresolved = new ArrayList<>();
    public static void planPaletteCombinedImage(ResourceLocation rl, IPalettePlan image) {
        planPaletteCombinedImage(rl, () -> image);
    }

    public static void planPaletteCombinedImage(ResourceLocation rl, Supplier<IPalettePlan> plan_sup) {
        miscResources.put(rl,IPalettePlan.supply(rl,plan_sup));
    }

    public static void planNativeImage(ResourceLocation rl, Supplier<NativeImage> supplier) {
        Supplier<InputStream> s = () -> {
            try (NativeImage image = supplier.get()) {
                if (image != null) {
                    return (InputStream) new ByteArrayInputStream(image.asByteArray());
                }
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Could not write buffered image to stream: {}...\n",rl, e);
            }
            return null;
        };
        miscResources.put(rl,ResettingSupplier.of(s,supplier));
    }

    public static Map<ResourceLocation, Supplier<InputStream>> getResources() {
        Map<ResourceLocation, Supplier<InputStream>> output = new HashMap<>(miscResources);
        for (Supplier<Map<ResourceLocation,Supplier<InputStream>>> l : unresolved) {
            output.putAll(l.get());
        }

        HashMap<ResourceLocation, String> sources = ClientPrePackRepository.getSourceJsons();
        for (ResourceLocation rl : sources.keySet()) {
            String s = sources.get(rl);
            if (s != null) {
                DynamicTextureJson source = DynamicTextureJson.fromJson(s);
                if (source != null) {
                    ResourceLocation orig_rl = ResourceLocation.of(source.output_location, ':');
                    ResourceLocation out_rl = new ResourceLocation(orig_rl.getNamespace(), "textures/" + orig_rl.getPath() + ".png");
                    Supplier<InputStream> sup = () -> {
                        try (NativeImage image = source.source.get()) {
                            if (image != null) {
                                return (InputStream) new ByteArrayInputStream(image.asByteArray());
                            }
                        } catch (IOException e) {
                            DynamicAssetGenerator.LOGGER.error("Could not write buffered image to stream: " + out_rl.toString());
                        } catch (JsonSyntaxException e) {
                            DynamicAssetGenerator.LOGGER.error("Issue loading texture source JSON: "+rl.toString());
                        }
                        return null;
                    };
                    output.put(out_rl, sup);
                }
            }
        }


        for (ResourceLocation rl : output.keySet()) {
            Supplier<InputStream> d = output.get(rl);
            if (d instanceof ResettingSupplier<InputStream> rs) {
                rs.reset();
            }
            if (DynamicAssetGenerator.getConfig().cacheAssets) {
                if (!(d instanceof WrappedSupplier<InputStream>)) {
                    Supplier<InputStream> supplier = new WrappedSupplier<>(() -> {
                        InputStream stream = d.get();
                        if (stream != null) {
                            try {
                                Path path = ModConfig.ASSET_CACHE_FOLDER.resolve(rl.getNamespace()).resolve(rl.getPath());
                                if (!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
                                if (!Files.exists(path)) {
                                    Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING);
                                }
                                return new BufferedInputStream(Files.newInputStream(path));
                            } catch (IOException e) {
                                DynamicAssetGenerator.LOGGER.error("Could not save asset...", e);
                            }
                        }
                        return stream;
                    });
                    output.put(rl, supplier);
                }
            }
        }

        return output;
    }

    public static void planLoadingStream(ResourceLocation location, Supplier<InputStream> sup) {
        if(DynamicAssetGenerator.getConfig().cacheAssets) {
            try {
                if (!Files.exists(ModConfig.ASSET_CACHE_FOLDER)) Files.createDirectories(ModConfig.ASSET_CACHE_FOLDER);
                if (Files.exists(ModConfig.ASSET_CACHE_FOLDER.resolve(location.getNamespace()).resolve(location.getPath()))) {
                    miscResources.put(location, () -> {
                        try {
                            File file = ModConfig.ASSET_CACHE_FOLDER.resolve(location.getNamespace()).resolve(location.getPath()).toFile();
                            if (file.isFile()) {
                                return new BufferedInputStream(new FileInputStream(file));
                            }
                        } catch (IOException e) {
                            DynamicAssetGenerator.LOGGER.error("Issue with cached asset...", e);
                        }
                        return null;
                    });
                    return;
                }
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Could not cache asset...",e);
            }
        }
        miscResources.put(location, sup);
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
