package com.github.lukebemish.dynamic_asset_generator.client.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import com.github.lukebemish.dynamic_asset_generator.client.api.json.DynamicTextureJson;
import com.github.lukebemish.dynamic_asset_generator.client.api.json.ITexSource;
import com.github.lukebemish.dynamic_asset_generator.client.util.SafeImageExtraction;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

public class Crop implements ITexSource {
    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Override
    public Supplier<BufferedImage> getSupplier(String inputStr) throws JsonSyntaxException {
        LocationSource locationSource = gson.fromJson(inputStr, LocationSource.class);
        Supplier<BufferedImage> input = DynamicTextureJson.readSupplierFromSource(locationSource.input);

        return () -> {
            if (input == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...");
                return null;
            }
            BufferedImage inImg = input.get();
            if (inImg == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", locationSource.input.toString());
                return null;
            }
            if (locationSource.total_size == 0) {
                DynamicAssetGenerator.LOGGER.error("Total image width must be non-zero");
            }
            int scale = inImg.getWidth()/locationSource.total_size;

            if (scale == 0) {
                DynamicAssetGenerator.LOGGER.error("Image scale turned out to be 0! Image is {} wide, total width is {}",
                        inImg.getWidth(),locationSource.total_size);
            }

            int distX = locationSource.size_x *scale;
            int distY = locationSource.size_y *scale;
            if (distY < 1 || distX < 1) {
                DynamicAssetGenerator.LOGGER.error("Bounds of image are negative! {}, {}",locationSource.size_x,locationSource.size_y);
                return null;
            }

            BufferedImage out = new BufferedImage(distX, distY, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < distX; x++) {
                for (int y = 0; y < distY; y++) {
                    int c = SafeImageExtraction.get(inImg,(x + locationSource.start_x*scale), (y + locationSource.start_y*scale));
                    out.setRGB(x, y, c);
                }
            }
            return out;
        };
    }

    public static class LocationSource {
        @Expose
        String source_type;
        @Expose
        int total_size;
        @Expose
        int start_x;
        @Expose
        int size_x;
        @Expose
        int start_y;
        @Expose
        int size_y;
        @Expose
        public JsonObject input;
    }
}
