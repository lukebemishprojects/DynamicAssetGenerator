package io.github.lukebemish.dynamic_asset_generator.api.client.generators.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.TexSourceDataHolder;
import io.github.lukebemish.dynamic_asset_generator.impl.client.NativeImageHelper;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.impl.client.util.SafeImageExtraction;

import java.util.function.Supplier;

public record Crop(int totalSize, int startX, int sizeX, int startY, int sizeY, ITexSource input) implements ITexSource {
    public static final Codec<Crop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("total_size").forGetter(Crop::totalSize),
            Codec.INT.fieldOf("start_x").forGetter(Crop::startX),
            Codec.INT.fieldOf("size_x").forGetter(Crop::sizeX),
            Codec.INT.fieldOf("start_y").forGetter(Crop::startY),
            Codec.INT.fieldOf("size_y").forGetter(Crop::sizeY),
            ITexSource.CODEC.fieldOf("input").forGetter(Crop::input)
    ).apply(instance, Crop::new));

    public Codec<Crop> codec() {
        return CODEC;
    }

    @Override
    public Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException {
        Supplier<NativeImage> suppliedInput = input().getSupplier(data);

        return () -> {
            if (suppliedInput == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", input());
                return null;
            }
            try (NativeImage inImg = suppliedInput.get()) {
                if (inImg == null) {
                    DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", input());
                    return null;
                }
                if (totalSize() == 0) {
                    DynamicAssetGenerator.LOGGER.error("Total image width must be non-zero");
                }
                int scale = inImg.getWidth() / totalSize();

                if (scale == 0) {
                    DynamicAssetGenerator.LOGGER.error("Image scale turned out to be 0! Image is {} wide, total width is {}",
                            inImg.getWidth(), totalSize());
                }

                int distX = sizeX() * scale;
                int distY = sizeY() * scale;
                if (distY < 1 || distX < 1) {
                    DynamicAssetGenerator.LOGGER.error("Bounds of image are negative! {}, {}", sizeX(), sizeY());
                    return null;
                }

                NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA, distX, distY, false);
                for (int x = 0; x < distX; x++) {
                    for (int y = 0; y < distY; y++) {
                        int c = SafeImageExtraction.get(inImg, (x + startX() * scale), (y + startY() * scale));
                        out.setPixelRGBA(x, y, c);
                    }
                }
                return out;
            }
        };
    }
}
