package io.github.lukebemish.dynamic_asset_generator.api.client.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.api.client.TexSourceDataHolder;
import io.github.lukebemish.dynamic_asset_generator.impl.client.NativeImageHelper;
import io.github.lukebemish.dynamic_asset_generator.api.client.ITexSource;

import java.util.List;
import java.util.function.Supplier;

public class ColorSource implements ITexSource {
    public static final Codec<ColorSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.listOf().fieldOf("color").forGetter(s->s.color)
    ).apply(instance,ColorSource::new));

    public ColorSource(List<Integer> color) {
        this.color = color;
    }

    private final List<Integer> color;

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException {
        return () -> {
            int len = Math.min(128*128,color.size());
            int sideLength = 0;
            for (int i = 0; i < 8; i++) {
                sideLength = (int) Math.pow(2,i);
                if (Math.pow(2,i)*Math.pow(2,i)>=len) {
                    break;
                }
            }
            NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA,sideLength,sideLength,false);
            outer:
            for (int y = 0; y < sideLength; y++) {
                for (int x = 0; x < sideLength; x++) {
                    if (x+sideLength*y >= len) {
                        break outer;
                    }
                    out.setPixelRGBA(x,y,color.get(x+sideLength*y));
                }
            }
            return out;
        };
    }
}