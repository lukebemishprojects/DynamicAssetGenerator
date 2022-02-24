package dynamic_asset_generator.client.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import dynamic_asset_generator.DynamicAssetGenerator;
import dynamic_asset_generator.client.api.PaletteExtractor;
import dynamic_asset_generator.client.api.json.DynamicTextureJson;
import dynamic_asset_generator.client.api.json.ITexSource;
import dynamic_asset_generator.client.palette.Palette;
import dynamic_asset_generator.client.util.IPalettePlan;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.Supplier;

public class ForegroundTransfer implements ITexSource {
    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Override
    public Supplier<BufferedImage> getSupplier(String inputStr) throws JsonSyntaxException {
        LocationSource lS = gson.fromJson(inputStr, LocationSource.class);
        Supplier<BufferedImage> background = DynamicTextureJson.readSupplierFromSource(lS.background);
        Supplier<BufferedImage> new_background = DynamicTextureJson.readSupplierFromSource(lS.new_background);
        Supplier<BufferedImage> full = DynamicTextureJson.readSupplierFromSource(lS.full);

        return () -> {
            if (background == null || full == null || new_background == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...");
                return null;
            }
            BufferedImage bImg = background.get();
            BufferedImage nImg = new_background.get();
            BufferedImage fImg = full.get();
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
            PaletteExtractor extractor = new PaletteExtractor(()->bImg,()->fImg,lS.extend_palette_size,lS.trim_trailing,lS.force_neighbors,lS.close_cutoff);
            PalettePlanner planner = PalettePlanner.of(lS, extractor, nImg);
            return Palette.paletteCombinedImage(planner);
        };
    }

    public static class PalettePlanner implements IPalettePlan {
        private final LocationSource info;
        private final BufferedImage nImg;
        private final PaletteExtractor extractor;

        private PalettePlanner(LocationSource info, PaletteExtractor extractor, BufferedImage nImg) {
            this.info = info;
            this.extractor = extractor;
            this.nImg = nImg;
        }

        public static PalettePlanner of(LocationSource info, PaletteExtractor extractor, BufferedImage nImg) {
            return new PalettePlanner(info, extractor, nImg);
        }

        @Override
        public BufferedImage getBackground() throws IOException {
            return nImg;
        }

        @Override
        public BufferedImage getOverlay() throws IOException {
            return extractor.getOverlayImg();
        }

        @Override
        public BufferedImage getPaletted() throws IOException {
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
