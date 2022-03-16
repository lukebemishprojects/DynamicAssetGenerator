package com.github.lukebemish.dynamic_asset_generator.client.api.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DynamicTextureJson {
    private static final Map<ResourceLocation, ITexSource> sources = new HashMap<>();
    private static final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Expose
    public String output_location;
    @Expose
    public JsonObject input;

    public Supplier<BufferedImage> source;

    @Nullable
    public static DynamicTextureJson fromJson(String json) throws JsonSyntaxException {
        DynamicTextureJson out = gson.fromJson(json, DynamicTextureJson.class);
        if (out.input != null && out.output_location != null) {
            Supplier<BufferedImage> buffer = readSupplierFromSource(out.input);
            if (buffer == null) return null;
            out.source = buffer;
        } else {
            DynamicAssetGenerator.LOGGER.error("Could not load JSON: {}", json);
        };
        return out;
    }

    public static Supplier<BufferedImage> readSupplierFromSource(JsonObject obj) throws JsonSyntaxException {
        if (obj.has("source_type") && obj.get("source_type").isJsonPrimitive() && obj.get("source_type").getAsJsonPrimitive().isString()) {
            String type = obj.get("source_type").getAsString();
            ResourceLocation input_type = ResourceLocation.of(type, ':');
            ITexSource source = sources.getOrDefault(input_type, null);
            if (source == null) {
                DynamicAssetGenerator.LOGGER.error("Unrecognized texture source type: " + type);
                return null;
            }
            Supplier<BufferedImage> buffer = source.getSupplier(obj.toString());
            if (buffer == null) {
                DynamicAssetGenerator.LOGGER.error("Bad input for source type: " + type);
                return null;
            }
            return buffer;
        }
        DynamicAssetGenerator.LOGGER.error("No valid source type found!");
        return null;
    }

    public static void registerTexSourceReadingType(ResourceLocation rl, ITexSource reader) {
        sources.put(rl, reader);
    }
}
