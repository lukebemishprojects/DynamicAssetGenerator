package io.github.lukebemish.dynamic_asset_generator.client.json;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.client.api.json.DynamicTextureJson;
import io.github.lukebemish.dynamic_asset_generator.client.api.json.ITexSource;

import java.util.function.Supplier;

public record FallbackSource(ITexSource original, ITexSource fallback) implements ITexSource {
    public static final Codec<FallbackSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DynamicTextureJson.TEXSOURCE_CODEC.fieldOf("original").forGetter(FallbackSource::original),
            DynamicTextureJson.TEXSOURCE_CODEC.fieldOf("fallback").forGetter(FallbackSource::fallback)
    ).apply(instance, FallbackSource::new));

    public Codec<FallbackSource> codec() {
        return CODEC;
    }

    @Override
    public Supplier<NativeImage> getSupplier() throws JsonSyntaxException{
        Supplier<NativeImage> original = this.original().getSupplier();
        Supplier<NativeImage> fallback = this.fallback().getSupplier();

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
}
