package io.github.lukebemish.dynamic_asset_generator.api.client.generators.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.TexSourceDataHolder;

import java.util.function.Supplier;

public record FallbackSource(ITexSource original, ITexSource fallback) implements ITexSource {
    public static final Codec<FallbackSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.CODEC.fieldOf("original").forGetter(FallbackSource::original),
            ITexSource.CODEC.fieldOf("fallback").forGetter(FallbackSource::fallback)
    ).apply(instance, FallbackSource::new));

    public Codec<FallbackSource> codec() {
        return CODEC;
    }

    @Override
    public Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException{
        Supplier<NativeImage> original = this.original().getSupplier(data);
        Supplier<NativeImage> fallback = this.fallback().getSupplier(data);

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
