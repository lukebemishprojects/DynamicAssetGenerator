package io.github.lukebemish.dynamic_asset_generator.api.client.generators.texsources.mask;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.TexSourceDataHolder;
import io.github.lukebemish.dynamic_asset_generator.impl.client.NativeImageHelper;
import io.github.lukebemish.dynamic_asset_generator.impl.client.palette.ColorHolder;
import io.github.lukebemish.dynamic_asset_generator.impl.client.util.SafeImageExtraction;
import io.github.lukebemish.dynamic_asset_generator.impl.util.MultiCloser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record MultiplyMask(List<ITexSource> sources) implements ITexSource {
    public static final Codec<MultiplyMask> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.CODEC.listOf().fieldOf("sources").forGetter(MultiplyMask::sources)
    ).apply(instance, MultiplyMask::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @NotNull Supplier<NativeImage> getSupplier(TexSourceDataHolder data) {
        List<Supplier<NativeImage>> inputs = new ArrayList<>();
        for (ITexSource o : this.sources()) {
            inputs.add(o.getSupplier(data));
        }
        return () -> {
            int maxX = 0;
            int maxY = 0;
            List<NativeImage> images = inputs.stream().map(Supplier::get).toList();
            for (int i = 0; i < images.size(); i++) {
                if (images.get(i)==null) {
                    data.getLogger().error("Texture given was nonexistent...\n{}",this.sources().get(i).toString());
                    return null;
                }
            }
            for (NativeImage image : images) {
                if (image.getWidth() > maxX) {
                    maxX = image.getWidth();
                    maxY = image.getHeight();
                }
            }
            try (MultiCloser ignored = new MultiCloser(images)) {
                NativeImage output = NativeImageHelper.of(NativeImage.Format.RGBA, maxX, maxY, false);
                NativeImage base = images.get(0);
                int xs = 1;
                int ys = 1;
                if (base.getWidth() / (base.getHeight() * 1.0) <= maxX / (maxY * 1.0)) {
                    xs = maxX / base.getWidth();
                    ys = maxY / base.getWidth();
                } else {
                    xs = maxX / base.getHeight();
                    ys = maxY / base.getHeight();
                }
                for (int x = 0; x < maxX; x++) {
                    for (int y = 0; y < maxY; y++) {
                        output.setPixelRGBA(x, y, SafeImageExtraction.get(base, x / xs, y / ys));
                    }
                }

                for (int x = 0; x < maxX; x++) {
                    for (int y = 0; y < maxY; y++) {
                        ColorHolder colorHolder = SafeImageExtraction.getColor(base, x / xs, y / ys);
                        if (images.size() >= 2) {
                            for (int i = 1; i < images.size(); i++) {
                                NativeImage image = images.get(i);
                                if (image.getWidth() / (image.getHeight() * 1.0) <= maxX / (maxY * 1.0)) {
                                    xs = maxX / image.getWidth();
                                    ys = maxY / image.getWidth();
                                } else {
                                    xs = maxX / image.getHeight();
                                    ys = maxY / image.getHeight();
                                }
                                ColorHolder top = ColorHolder.fromColorInt(SafeImageExtraction.get(image, x / xs, y / ys));
                                colorHolder = new ColorHolder(
                                        colorHolder.getR()*top.getR(),
                                        colorHolder.getG()*top.getG(),
                                        colorHolder.getB()*top.getB(),
                                        colorHolder.getA()*top.getA());
                            }
                        }
                        output.setPixelRGBA(x,y,colorHolder.toInt());
                    }
                }
                return output;
            }
        };
    }
}
