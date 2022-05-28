package io.github.lukebemish.dynamic_asset_generator.client.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.client.api.json.DynamicTextureJson;
import io.github.lukebemish.dynamic_asset_generator.client.api.json.ITexSource;

import java.util.function.Supplier;

public class FallbackSource implements ITexSource {
    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Override
    public Supplier<NativeImage> getSupplier(String inputStr) throws JsonSyntaxException{
        LocationSource locationSource = gson.fromJson(inputStr, LocationSource.class);
        Supplier<NativeImage> original = DynamicTextureJson.readSupplierFromSource(locationSource.original);
        Supplier<NativeImage> fallback = DynamicTextureJson.readSupplierFromSource(locationSource.fallback);

        return () -> {
            if (original != null) {
                NativeImage img = original.get();
                if (img != null) return img;
                DynamicAssetGenerator.LOGGER.debug("Issue loading main texture, trying fallback");
            }
            if (fallback != null) {
                NativeImage img = fallback.get();
                if (img != null) return img;
            }
            DynamicAssetGenerator.LOGGER.warn("Texture given was nonexistent...");
            return null;
        };
    }

    public static class LocationSource {
        @Expose
        String source_type;
        @Expose
        public JsonObject original;
        @Expose
        public JsonObject fallback;
    }
}
