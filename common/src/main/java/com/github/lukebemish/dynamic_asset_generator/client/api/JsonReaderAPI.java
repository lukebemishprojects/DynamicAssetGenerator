package com.github.lukebemish.dynamic_asset_generator.client.api;

import com.github.lukebemish.dynamic_asset_generator.client.api.json.DynamicTextureJson;
import com.github.lukebemish.dynamic_asset_generator.client.api.json.ITexSource;
import net.minecraft.resources.ResourceLocation;

public class JsonReaderAPI {
    public static void registerTexSourceReadingType(ResourceLocation rl, ITexSource reader) {
        DynamicTextureJson.registerTexSourceReadingType(rl,reader);
    }
}
