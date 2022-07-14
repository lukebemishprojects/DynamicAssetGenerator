package io.github.lukebemish.dynamic_asset_generator.api.client.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.api.client.TexSourceDataHolder;
import io.github.lukebemish.dynamic_asset_generator.impl.client.NativeImageHelper;
import io.github.lukebemish.dynamic_asset_generator.api.client.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.impl.client.util.SafeImageExtraction;

import java.util.function.Supplier;

public record Transform(ITexSource input, int rotate, boolean flip) implements ITexSource {
    public static final Codec<Transform> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.CODEC.fieldOf("input").forGetter(Transform::input),
            Codec.INT.fieldOf("rotate").forGetter(Transform::rotate),
            Codec.BOOL.fieldOf("flip").forGetter(Transform::flip)
    ).apply(instance, Transform::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException {
        Supplier<NativeImage> input = this.input().getSupplier(data);

        return () -> {
            if (input == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...");
                return null;
            }
            NativeImage inImg = input.get();
            if (inImg == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", this.input().toString());
                return null;
            }
            NativeImage output = inImg;
            for (int i = 0; i < this.rotate(); i++) {
                output = clockwiseRotate(output);
            }
            if (this.flip()) {
                NativeImage output2 = NativeImageHelper.of(output.format(), output.getWidth(), output.getHeight(), false);
                for (int x = 0; x < output.getWidth(); x++) {
                    for (int y = 0; y < output.getHeight(); y++) {
                        output2.setPixelRGBA((output.getWidth()-1-x),y, SafeImageExtraction.get(output,x,y));
                    }
                }
                output.close();
                output = output2;
            }
            return output;
        };
    }

    private static NativeImage clockwiseRotate(NativeImage input) {
        int w = input.getWidth();
        int h = input.getHeight();
        NativeImage output = NativeImageHelper.of(input.format(), h, w, false);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                output.setPixelRGBA(y, w - x - 1, SafeImageExtraction.get(input,x, y));
        input.close();
        return output;
    }
}
