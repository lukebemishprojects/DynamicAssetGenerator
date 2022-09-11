package io.github.lukebemish.dynamic_asset_generator.api.client.generators.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.TexSourceDataHolder;
import io.github.lukebemish.dynamic_asset_generator.impl.client.NativeImageHelper;
import io.github.lukebemish.dynamic_asset_generator.impl.client.palette.ColorHolder;
import io.github.lukebemish.dynamic_asset_generator.impl.client.util.SafeImageExtraction;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record Mask(ITexSource input, ITexSource mask) implements ITexSource {
    public static final Codec<Mask> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.CODEC.fieldOf("input").forGetter(Mask::input),
            ITexSource.CODEC.fieldOf("mask").forGetter(Mask::mask)
    ).apply(instance, Mask::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @NotNull Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException {
        Supplier<NativeImage> input = this.input().getSupplier(data);
        Supplier<NativeImage> mask = this.mask().getSupplier(data);

        return () -> {
            try (NativeImage inImg = input.get();
                 NativeImage maskImg = mask.get()) {
                if (maskImg == null) {
                    data.getLogger().error("Texture given was nonexistent...\n{}", this.mask());
                    return null;
                }
                if (inImg == null) {
                    data.getLogger().error("Texture given was nonexistent...\n{}", this.input());
                    return null;
                }
                int maxX = Math.max(inImg.getWidth(), maskImg.getWidth());
                int maxY = inImg.getWidth() > maskImg.getWidth() ? inImg.getHeight() : maskImg.getHeight();
                int mxs, mys, ixs, iys;
                if (maskImg.getWidth() / (maskImg.getHeight() * 1.0) <= maxX / (maxY * 1.0)) {
                    mxs = maxX / maskImg.getWidth();
                    mys = maxY / maskImg.getWidth();
                } else {
                    mxs = maxX / maskImg.getHeight();
                    mys = maxY / maskImg.getHeight();
                }
                if (inImg.getWidth() / (inImg.getHeight() * 1.0) <= maxX / (maxY * 1.0)) {
                    ixs = inImg.getWidth() / maxX;
                    iys = inImg.getWidth() / maxY;
                } else {
                    ixs = inImg.getHeight() / maxX;
                    iys = inImg.getHeight() / maxY;
                }
                NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA, maxX, maxY, false);
                for (int x = 0; x < maxX; x++) {
                    for (int y = 0; y < maxY; y++) {
                        ColorHolder mC = ColorHolder.fromColorInt(SafeImageExtraction.get(maskImg, x / mxs, y / mys));
                        ColorHolder iC = ColorHolder.fromColorInt(SafeImageExtraction.get(inImg, x / ixs, y / iys));
                        ColorHolder o = iC.withA(mC.getA() * iC.getA());
                        out.setPixelRGBA(x, y, ColorHolder.toColorInt(o));
                    }
                }
                return out;
            }
        };
    }
}
