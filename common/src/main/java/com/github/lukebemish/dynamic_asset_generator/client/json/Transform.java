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

public class Transform implements ITexSource {
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
            BufferedImage output = inImg;
            for (int i = 0; i < locationSource.rotate; i++) {
                output = clockwiseRotate(output);
            }
            if (locationSource.flip) {
                BufferedImage output2 = new BufferedImage(output.getWidth(), output.getHeight(), output.getType());
                for (int x = 0; x < output.getWidth(); x++) {
                    for (int y = 0; y < output.getHeight(); y++) {
                        output2.setRGB((output.getWidth()-1-x),y,SafeImageExtraction.get(output,x,y));
                    }
                }
                output = output2;
            }
            return output;
        };
    }

    private static BufferedImage clockwiseRotate(BufferedImage input) {
        int w = input.getWidth();
        int h = input.getHeight();
        BufferedImage output = new BufferedImage(h, w, input.getType());
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                output.setRGB(y, w - x - 1, SafeImageExtraction.get(input,x, y));
        return output;
    }

    public static class LocationSource {
        @Expose
        String source_type;
        @Expose
        int rotate;
        @Expose
        boolean flip;
        @Expose
        public JsonObject input;
    }
}
