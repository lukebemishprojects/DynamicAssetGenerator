package com.github.lukebemish.dynamic_asset_generator.client.json;

import com.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import com.github.lukebemish.dynamic_asset_generator.client.NativeImageHelper;
import com.github.lukebemish.dynamic_asset_generator.client.api.json.DynamicTextureJson;
import com.github.lukebemish.dynamic_asset_generator.client.api.json.ITexSource;
import com.github.lukebemish.dynamic_asset_generator.client.util.SafeImageExtraction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.mojang.blaze3d.platform.NativeImage;

import java.util.function.Supplier;

public class Transform implements ITexSource {
    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Override
    public Supplier<NativeImage> getSupplier(String inputStr) throws JsonSyntaxException {
        LocationSource locationSource = gson.fromJson(inputStr, LocationSource.class);
        Supplier<NativeImage> input = DynamicTextureJson.readSupplierFromSource(locationSource.input);

        return () -> {
            if (input == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...");
                return null;
            }
            NativeImage inImg = input.get();
            if (inImg == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", locationSource.input.toString());
                return null;
            }
            NativeImage output = inImg;
            for (int i = 0; i < locationSource.rotate; i++) {
                output = clockwiseRotate(output);
            }
            if (locationSource.flip) {
                NativeImage output2 = NativeImageHelper.of(output.format(), output.getWidth(), output.getHeight(), false);
                for (int x = 0; x < output.getWidth(); x++) {
                    for (int y = 0; y < output.getHeight(); y++) {
                        output2.setPixelRGBA((output.getWidth()-1-x),y,SafeImageExtraction.get(output,x,y));
                    }
                }
                output.close();
                output = output2;
            }
            return output;
        };
    }

    private static NativeImage clockwiseRotate(NativeImage input) {
        int w = input.getWidth();
        int h = input.getHeight();
        NativeImage output = NativeImageHelper.of(input.format(), h, w, false);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                output.setPixelRGBA(y, w - x - 1, SafeImageExtraction.get(input,x, y));
        input.close();
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
