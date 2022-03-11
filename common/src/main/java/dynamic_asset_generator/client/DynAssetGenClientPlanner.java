package dynamic_asset_generator.client;

import com.google.gson.JsonSyntaxException;
import dynamic_asset_generator.DynamicAssetGenerator;
import dynamic_asset_generator.ModConfig;
import dynamic_asset_generator.api.ResettingSupplier;
import dynamic_asset_generator.client.api.ClientPrePackRepository;
import dynamic_asset_generator.client.api.json.DynamicTextureJson;
import dynamic_asset_generator.client.palette.Palette;
import dynamic_asset_generator.client.util.IPalettePlan;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DynAssetGenClientPlanner {
    private static final Map<ResourceLocation, Supplier<IPalettePlan>> plannedPaletteCombinedImages = new HashMap<>();
    private static final Map<ResourceLocation, Supplier<InputStream>> miscResources = new HashMap<>();
    private static final Map<ResourceLocation, Supplier<BufferedImage>> bufferMap = new HashMap<>();

    public static void planPaletteCombinedImage(ResourceLocation rl, IPalettePlan image) {
        planPaletteCombinedImage(rl, () -> image);
    }

    public static void planPaletteCombinedImage(ResourceLocation rl, Supplier<IPalettePlan> image) {
        if(DynamicAssetGenerator.getConfig().cacheAssets) {
            try {
                if (!Files.exists(ModConfig.ASSET_CACHE_FOLDER)) Files.createDirectories(ModConfig.ASSET_CACHE_FOLDER);
                if (Files.exists(ModConfig.ASSET_CACHE_FOLDER.resolve(rl.getNamespace()).resolve(rl.getPath()))) {
                    miscResources.put(rl, () -> {
                        try {
                            File file = ModConfig.ASSET_CACHE_FOLDER.resolve(rl.getNamespace()).resolve(rl.getPath()).toFile();
                            if (file.isFile()) {
                                return new FileInputStream(file);
                            }
                        } catch (IOException ignored) {
                        }
                        return null;
                    });
                    return;
                }
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Could not cache asset...");
            }
        }
        plannedPaletteCombinedImages.put(rl, image);
    }

    public static void planBufferedImage(ResourceLocation rl, Supplier<BufferedImage> supplier) {
        if(DynamicAssetGenerator.getConfig().cacheAssets) {
            try {
                if (!Files.exists(ModConfig.ASSET_CACHE_FOLDER)) Files.createDirectories(ModConfig.ASSET_CACHE_FOLDER);
                if (Files.exists(ModConfig.ASSET_CACHE_FOLDER.resolve(rl.getNamespace()).resolve(rl.getPath()))) {
                    miscResources.put(rl, () -> {
                        try {
                            File file = ModConfig.ASSET_CACHE_FOLDER.resolve(rl.getNamespace()).resolve(rl.getPath()).toFile();
                            if (file.isFile()) {
                                return new FileInputStream(file);
                            }
                        } catch (IOException ignored) {
                        }
                        return null;
                    });
                    return;
                }
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Could not cache asset...");
            }
        }
        bufferMap.put(rl, supplier);
    }

    public static Map<ResourceLocation, Supplier<InputStream>> getResources() {
        Map<ResourceLocation, Supplier<InputStream>> output = new HashMap<>();
        for (ResourceLocation key : plannedPaletteCombinedImages.keySet()) {
            Supplier<InputStream> s = () -> {
                IPalettePlan planned = plannedPaletteCombinedImages.get(key).get();
                BufferedImage image = Palette.paletteCombinedImage(planned);
                if (image != null) {
                    try {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        ImageIO.write(image, "png", os);
                        return (InputStream) new ByteArrayInputStream(os.toByteArray());
                    } catch (IOException e) {
                        DynamicAssetGenerator.LOGGER.error("Could not write buffered image to stream: " + key.toString());
                    }
                }
                return null;
            };
            output.put(key, s);
        }
        for (ResourceLocation key : bufferMap.keySet()) {
            Supplier<InputStream> s = () -> {
            BufferedImage image = bufferMap.get(key).get();
                if (image != null) {
                    try {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        ImageIO.write(image, "png", os);
                        InputStream is = new ByteArrayInputStream(os.toByteArray());
                        return is;
                    } catch (IOException e) {
                        DynamicAssetGenerator.LOGGER.error("Could not write buffered image to stream: " + key.toString());
                    }
                }
                return null;
            };
            output.put(key, s);
        }
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
                        BufferedImage image = source.source.get();
                        if (image != null) {
                            try {
                                ByteArrayOutputStream os = new ByteArrayOutputStream();
                                ImageIO.write(image, "png", os);
                                InputStream is = new ByteArrayInputStream(os.toByteArray());
                                return is;
                            } catch (IOException e) {
                                DynamicAssetGenerator.LOGGER.error("Could not write buffered image to stream: " + out_rl.toString());
                            } catch (JsonSyntaxException e) {
                                DynamicAssetGenerator.LOGGER.error("Issue loading texture source JSON: "+rl.toString());
                            }
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
                InputStream stream = d.get();
                if (stream != null) {
                    try {
                        Path path = ModConfig.ASSET_CACHE_FOLDER.resolve(rl.getNamespace()).resolve(rl.getPath());
                        if (!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
                        if (!Files.exists(path)) {
                            Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        DynamicAssetGenerator.LOGGER.error("Could not save asset...", e);
                    }
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
                                return new FileInputStream(file);
                            }
                        } catch (IOException e) {
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
