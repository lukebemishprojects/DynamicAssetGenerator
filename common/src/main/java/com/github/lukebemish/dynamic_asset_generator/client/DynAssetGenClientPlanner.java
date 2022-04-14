package com.github.lukebemish.dynamic_asset_generator.client;

import com.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import com.github.lukebemish.dynamic_asset_generator.ModConfig;
import com.github.lukebemish.dynamic_asset_generator.WrappedSupplier;
import com.github.lukebemish.dynamic_asset_generator.api.ResettingSupplier;
import com.github.lukebemish.dynamic_asset_generator.client.api.ClientPrePackRepository;
import com.github.lukebemish.dynamic_asset_generator.client.api.json.DynamicTextureJson;
import com.github.lukebemish.dynamic_asset_generator.client.palette.Palette;
import com.github.lukebemish.dynamic_asset_generator.client.util.IPalettePlan;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DynAssetGenClientPlanner {
    private static final Map<ResourceLocation, Supplier<InputStream>> miscResources = new HashMap<>();

    public static void planPaletteCombinedImage(ResourceLocation rl, IPalettePlan image) {
        planPaletteCombinedImage(rl, () -> image);
    }

    public static void planPaletteCombinedImage(ResourceLocation rl, Supplier<IPalettePlan> plan_sup) {
        Supplier<InputStream> s = () -> {
            IPalettePlan planned = plan_sup.get();
            try (NativeImage image = Palette.paletteCombinedImage(planned)) {
                if (image != null) {
                    return (InputStream) new ByteArrayInputStream(image.asByteArray());
                }
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Could not write buffered image to stream: {}...\n",rl, e);
            }
            return null;
        };
        miscResources.put(rl,ResettingSupplier.of(s,plan_sup));
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
        Map<ResourceLocation, Supplier<InputStream>> output = new HashMap<>();
        for (ResourceLocation key : miscResources.keySet()) {
            if (miscResources.get(key) instanceof ResettingSupplier<InputStream> rs) {
                rs.reset();
            }
            output.put(key, miscResources.get(key));
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
}
