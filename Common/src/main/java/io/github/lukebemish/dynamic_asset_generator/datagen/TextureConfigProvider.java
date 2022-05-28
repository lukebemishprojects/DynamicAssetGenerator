package io.github.lukebemish.dynamic_asset_generator.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.data.*;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public abstract class TextureConfigProvider implements DataProvider {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();

    private final DataGenerator generator;
    private final String modid;
    private final Map<ResourceLocation, TextureConfig> generatedConfigs = new HashMap<>();

    public TextureConfigProvider(DataGenerator generator, String modid) {
        this.generator = generator;
        this.modid = modid;
    }

    public abstract void addConfigs();

    public final TextureConfig config(String path) {
        return generatedConfigs.computeIfAbsent(
                new ResourceLocation(modid, path),
                loc -> new TextureConfig()
        );
    }

    public final ImageSource.File fileSource(ResourceLocation texture) {
        return new ImageSource.File(this, texture);
    }

    public final ImageSource.Fallback fallbackSource() {
        return new ImageSource.Fallback(this);
    }

    public final ImageSource.Color colorSource() { return new ImageSource.Color(this); }

    public final ImageSource.Overlay overlaySource() { return new ImageSource.Overlay(this); }

    public final ImageSource.Mask maskSource() { return new ImageSource.Mask(this); }

    public final ImageSource.Crop cropSource() { return new ImageSource.Crop(this); }

    public final ImageSource.Transform transformSource() { return new ImageSource.Transform(this); }

    public final ImageSource.CombinedPalettedImage combinedPalettedImageSource() {
        return new ImageSource.CombinedPalettedImage(this);
    }

    public final ImageSource.ForegroundTransfer foregroundTransferSource() {
        return new ImageSource.ForegroundTransfer(this);
    }

    protected boolean checkTextureExists(ResourceLocation texture) { return true; }

    @Override
    public final void run(HashCache cache) {
        generatedConfigs.clear();
        addConfigs();
        writeConfigs(cache);
    }

    private void writeConfigs(HashCache cache) {
        for (ResourceLocation location : generatedConfigs.keySet()) {
            Path target = generator.getOutputFolder().resolve(String.format(
                    "assets/%s/dynamic_assets_sources/%s.json",
                    location.getNamespace(),
                    location.getPath()
            ));

            try {
                DataProvider.save(GSON, cache, generatedConfigs.get(location).toJson(), target);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getName() { return "Texture Configs"; }
}
