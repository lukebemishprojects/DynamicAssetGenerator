package com.github.lukebemish.dynamic_asset_generator.client.json;

import com.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import com.github.lukebemish.dynamic_asset_generator.client.NativeImageHelper;
import com.github.lukebemish.dynamic_asset_generator.client.api.json.DynamicTextureJson;
import com.github.lukebemish.dynamic_asset_generator.client.api.json.ITexSource;
import com.github.lukebemish.dynamic_asset_generator.client.palette.ColorHolder;
import com.github.lukebemish.dynamic_asset_generator.client.util.SafeImageExtraction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.mojang.blaze3d.platform.NativeImage;

import java.util.function.Supplier;

public class Mask implements ITexSource {
    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Override
    public Supplier<NativeImage> getSupplier(String inputStr) throws JsonSyntaxException {
        LocationSource locationSource = gson.fromJson(inputStr, LocationSource.class);
        Supplier<NativeImage> input = DynamicTextureJson.readSupplierFromSource(locationSource.input);
        Supplier<NativeImage> mask = DynamicTextureJson.readSupplierFromSource(locationSource.mask);

        return () -> {
            if (input == null || mask == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...");
                return null;
            }
            NativeImage inImg = input.get();
            NativeImage maskImg = mask.get();
            if (maskImg == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", locationSource.mask.toString());
                return null;
            }
            if (inImg == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", locationSource.input.toString());
                return null;
            }
            int maxX = Math.max(inImg.getWidth(),maskImg.getWidth());
            int maxY = inImg.getWidth() > maskImg.getWidth() ? inImg.getHeight() : maskImg.getHeight();
            int mxs,mys,ixs,iys;
            if (maskImg.getWidth() / (maskImg.getHeight() * 1.0) <= maxX / (maxY * 1.0)) {
                mxs = maxX/maskImg.getWidth();
                mys = maxY/maskImg.getWidth();
            } else {
                mxs = maxX/maskImg.getHeight();
                mys = maxY/maskImg.getHeight();
            }
            if (inImg.getWidth() / (inImg.getHeight() * 1.0) <= maxX / (maxY * 1.0)) {
                ixs = inImg.getWidth()/maxX;
                iys = inImg.getWidth()/maxY;
            } else {
                ixs = inImg.getHeight()/maxX;
                iys = inImg.getHeight()/maxY;
            }
            NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA, maxX, maxY, false);
            for (int x = 0; x < maxX; x++) {
                for (int y = 0; y < maxY; y++) {
                    ColorHolder mC = ColorHolder.fromColorInt(SafeImageExtraction.get(maskImg,x/mxs,y/mys));
                    ColorHolder iC = ColorHolder.fromColorInt(SafeImageExtraction.get(inImg,x/ixs,y/iys));
                    ColorHolder o = iC.withA(mC.getA() * iC.getA());
                    out.setPixelRGBA(x,y,ColorHolder.toColorInt(o));
                }
            }
            return out;
        };
    }

    public static class LocationSource {
        @Expose
        String source_type;
        @Expose
        public JsonObject input;
        @Expose
        public JsonObject mask;
    }
}
