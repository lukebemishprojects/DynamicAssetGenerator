package io.github.lukebemish.dynamic_asset_generator.api.client.generators.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.TexSourceDataHolder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

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
    public @NotNull Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException{
        TexSourceDataHolder newData = new TexSourceDataHolder(data);
        newData.put(Logger.class, NOPLogger.NOP_LOGGER);
        Supplier<NativeImage> original = this.original().getSupplier(newData);
        Supplier<NativeImage> fallback = this.fallback().getSupplier(data);

        return () -> {
            NativeImage img = original.get();
            if (img != null) return img;
            data.getLogger().debug("Issue loading main texture, trying fallback");
            img = fallback.get();
            if (img != null) return img;
            data.getLogger().error("Texture given was nonexistent...");
            return null;
        };
    }
}
