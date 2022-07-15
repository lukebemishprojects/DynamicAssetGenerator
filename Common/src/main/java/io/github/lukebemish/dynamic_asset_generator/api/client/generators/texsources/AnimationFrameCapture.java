package io.github.lukebemish.dynamic_asset_generator.api.client.generators.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.TexSourceDataHolder;
import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record AnimationFrameCapture(String capture) implements ITexSource {
    public static final Codec<AnimationFrameCapture> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("capture").forGetter(AnimationFrameCapture::capture)
    ).apply(instance, AnimationFrameCapture::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @NotNull Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException {
        return () -> {
            AnimationSplittingSource.ImageCollection collection = data.get(AnimationSplittingSource.ImageCollection.class);
            if (collection==null) {
                DynamicAssetGenerator.LOGGER.error("No parent animation source to capture...");
                return null;
            }
            NativeImage image = collection.get(this.capture());
            if (image==null) {
                DynamicAssetGenerator.LOGGER.error("Key '{}' was not supplied to capture...",capture());
            }
            return image;
        };
    }
}
