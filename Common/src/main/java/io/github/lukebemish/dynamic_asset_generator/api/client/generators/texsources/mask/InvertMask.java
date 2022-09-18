package io.github.lukebemish.dynamic_asset_generator.api.client.generators.texsources.mask;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.TexSourceDataHolder;
import io.github.lukebemish.dynamic_asset_generator.impl.client.NativeImageHelper;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record InvertMask(ITexSource source) implements ITexSource {
    public static final Codec<InvertMask> CODEC = RecordCodecBuilder.create(i -> i.group(
            ITexSource.CODEC.fieldOf("source").forGetter(InvertMask::source)
    ).apply(i, InvertMask::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @NotNull Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException {
        Supplier<NativeImage> input = this.source.getSupplier(data);
        return () -> {
            try (NativeImage inImg = input.get()) {
                if (inImg == null) {
                    data.getLogger().error("Texture given was nonexistent...\n{}", this.source);
                    return null;
                }
                int width = inImg.getWidth();
                int height = inImg.getHeight();
                NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA, width, height, false);
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < width; y++) {
                        int source = inImg.getPixelRGBA(x, y);
                        out.setPixelRGBA(x, y, ~source);
                    }
                }
                return out;
            }
        };
    }
}
