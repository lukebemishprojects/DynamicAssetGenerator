package io.github.lukebemish.dynamic_asset_generator.client.api.json;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;

import java.util.function.Supplier;

public interface ITexSource {
    Codec<? extends ITexSource> codec();
    Supplier<NativeImage> getSupplier() throws JsonSyntaxException;
}
