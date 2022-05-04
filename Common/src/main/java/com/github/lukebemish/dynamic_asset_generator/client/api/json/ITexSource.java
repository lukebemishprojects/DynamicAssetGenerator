package com.github.lukebemish.dynamic_asset_generator.client.api.json;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;

import java.util.function.Supplier;

public interface ITexSource {
    Supplier<NativeImage> getSupplier(String inputStr) throws JsonSyntaxException;
}
