package com.github.lukebemish.dynamic_asset_generator.client.json;

import com.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import com.github.lukebemish.dynamic_asset_generator.client.api.PaletteExtractor;
import com.github.lukebemish.dynamic_asset_generator.client.api.json.DynamicTextureJson;
import com.github.lukebemish.dynamic_asset_generator.client.api.json.ITexSource;
import com.github.lukebemish.dynamic_asset_generator.client.palette.Palette;
import com.github.lukebemish.dynamic_asset_generator.client.util.IPalettePlan;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;
import java.util.function.Supplier;

public class ForegroundTransfer implements ITexSource {
    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Override
    public Supplier<NativeImage> getSupplier(String inputStr) throws JsonSyntaxException {
        LocationSource lS = gson.fromJson(inputStr, LocationSource.class);
        Supplier<NativeImage> background = DynamicTextureJson.readSupplierFromSource(lS.background);
        Supplier<NativeImage> new_background = DynamicTextureJson.readSupplierFromSource(lS.new_background);
        Supplier<NativeImage> full = DynamicTextureJson.readSupplierFromSource(lS.full);

        return () -> {
            if (background == null || full == null || new_background == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...");
                return null;
            }
            try (NativeImage bImg = background.get();
                 NativeImage nImg = new_background.get();
                 NativeImage fImg = full.get()) {
                if (bImg == null) {
                    DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", lS.background.toString());
                    return null;
                }
                if (nImg == null) {
                    DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", lS.new_background.toString());
                    return null;
                }
                if (fImg == null) {
                    DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", lS.full.toString());
                    return null;
                }
                PaletteExtractor extractor = new PaletteExtractor(() -> bImg, () -> fImg, lS.extend_palette_size, lS.trim_trailing, lS.force_neighbors, lS.close_cutoff);
                PalettePlanner planner = PalettePlanner.of(lS, extractor, nImg);
                return Palette.paletteCombinedImage(planner);
            }
        };
    }

    public static class PalettePlanner implements IPalettePlan {
        private final LocationSource info;
        private final NativeImage nImg;
        private final PaletteExtractor extractor;

        private PalettePlanner(LocationSource info, PaletteExtractor extractor, NativeImage nImg) {
            this.info = info;
            this.extractor = extractor;
            this.nImg = nImg;
        }

        public static PalettePlanner of(LocationSource info, PaletteExtractor extractor, NativeImage nImg) {
            return new PalettePlanner(info, extractor, nImg);
        }

        @Override
        public NativeImage getBackground() throws IOException {
            return nImg;
        }

        @Override
        public NativeImage getOverlay() throws IOException {
            return extractor.getOverlayImg();
        }

        @Override
        public NativeImage getPaletted() throws IOException {
            return extractor.getPalettedImg();
        }

        @Override
        public boolean includeBackground() {
            return true;
        }

        @Override
        public boolean stretchPaletted() {
            return false;
        }

        @Override
        public int extend() {
            return extractor.extend;
        }
    }

    public static class LocationSource {
        @Expose
        String source_type;
        @Expose
        public JsonObject background;
        @Expose
        public JsonObject full;
        @Expose
        public JsonObject new_background;
        @Expose
        int extend_palette_size;
        @Expose
        boolean trim_trailing;
        @Expose
        boolean force_neighbors;
        @Expose
        double close_cutoff;
    }
}
