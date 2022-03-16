package com.github.lukebemish.dynamic_asset_generator.client.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import com.github.lukebemish.dynamic_asset_generator.client.api.json.DynamicTextureJson;
import com.github.lukebemish.dynamic_asset_generator.client.api.json.ITexSource;
import com.github.lukebemish.dynamic_asset_generator.client.palette.ColorHolder;
import com.github.lukebemish.dynamic_asset_generator.client.util.SafeImageExtraction;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Overlay implements ITexSource {
    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Override
    public Supplier<BufferedImage> getSupplier(String inputStr) throws JsonSyntaxException {
        LocationSource locationSource = gson.fromJson(inputStr, LocationSource.class);
        List<Supplier<BufferedImage>> inputs = new ArrayList<>();
        for (JsonObject o : locationSource.inputs) {
            inputs.add(DynamicTextureJson.readSupplierFromSource(o));
        }
        return () -> {
            int maxX = 0;
            int maxY = 0;
            List<BufferedImage> images = inputs.stream().map(Supplier::get).toList();
            for (int i = 0; i < images.size(); i++) {
                if (images.get(i)==null) {
                    DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}",locationSource.inputs.get(i).toString());
                    return null;
                }
            }
            for (BufferedImage image : images) {
                if (image.getWidth() > maxX) {
                    maxX = image.getWidth();
                    maxY = image.getHeight();
                }
            }
            BufferedImage output = new BufferedImage(maxX, maxY, BufferedImage.TYPE_INT_ARGB);
            BufferedImage base = images.get(0);
            int xs = 1;
            int ys = 1;
            if (base.getWidth() / (base.getHeight() * 1.0) <= maxX / (maxY * 1.0)) {
                xs = maxX/base.getWidth();
                ys = maxY/base.getWidth();
            } else {
                xs = maxX/base.getHeight();
                ys = maxY/base.getHeight();
            }
            for (int x = 0; x < maxX; x++) {
                for (int y = 0; y < maxY; y++) {
                    output.setRGB(x,y, SafeImageExtraction.get(base,x/xs, y/ys));
                }
            }
            if (images.size() >= 2) {
                for (int i = 1; i < images.size(); i++) {
                    BufferedImage image = images.get(i);
                    if (image.getWidth() / (image.getHeight() * 1.0) <= maxX / (maxY * 1.0)) {
                        xs = maxX/image.getWidth();
                        ys = maxY/image.getWidth();
                    } else {
                        xs = maxX/image.getHeight();
                        ys = maxY/image.getHeight();
                    }
                    for (int x = 0; x < maxX; x++) {
                        for (int y = 0; y < maxY; y++) {
                            ColorHolder input = ColorHolder.fromColorInt(SafeImageExtraction.get(output,x,y));
                            ColorHolder top = ColorHolder.fromColorInt(SafeImageExtraction.get(image,x/xs,y/ys));
                            ColorHolder outColor = ColorHolder.alphaBlend(top,input);
                            output.setRGB(x,y, ColorHolder.toColorInt(outColor));
                        }
                    }
                }
            }
            return output;
        };
    }

    public static class LocationSource {
        @Expose
        String source_type;
        @Expose
        public List<JsonObject> inputs;
    }
}
