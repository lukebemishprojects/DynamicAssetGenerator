package io.github.lukebemish.dynamic_asset_generator.api.client.generators.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.TexSourceDataHolder;
import io.github.lukebemish.dynamic_asset_generator.impl.client.NativeImageHelper;
import io.github.lukebemish.dynamic_asset_generator.impl.client.palette.ColorHolder;
import io.github.lukebemish.dynamic_asset_generator.impl.client.palette.Palette;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record PaletteSpreadSource(ITexSource source, float paletteCutoff, float lowerBound, float upperBound) implements ITexSource {
    public static final Codec<PaletteSpreadSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            ITexSource.CODEC.fieldOf("source").forGetter(PaletteSpreadSource::source),
            Codec.FLOAT.optionalFieldOf("palette_cutoff", Palette.DEFAULT_CUTOFF).forGetter(PaletteSpreadSource::paletteCutoff),
            Codec.either(Codec.INT, Codec.FLOAT).xmap(e->e.map(x->x/255f,f->f), Either::right).optionalFieldOf("lower_bound",0f).forGetter(PaletteSpreadSource::lowerBound),
            Codec.either(Codec.INT, Codec.FLOAT).xmap(e->e.map(x->x/255f,f->f), Either::right).optionalFieldOf("upper_bound",1f).forGetter(PaletteSpreadSource::lowerBound)
    ).apply(i,PaletteSpreadSource::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @NotNull Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException {
        return () -> {
            Supplier<NativeImage> source = source().getSupplier(data);
            try (NativeImage sourceImg = source.get()) {
                if (sourceImg == null) {
                    data.getLogger().error("Texture given was nonexistent...\n{}", this.source());
                    return null;
                }
                Palette palette = Palette.extractPalette(sourceImg, 0, paletteCutoff());
                NativeImage outImg = NativeImageHelper.of(NativeImage.Format.RGBA, sourceImg.getWidth(), sourceImg.getHeight(), false);

                int maxIndex = palette.getSize()-1;
                float diff = upperBound()-lowerBound();

                for (int x = 0; x < sourceImg.getWidth(); x++) {
                    for (int y = 0; y < sourceImg.getHeight(); y++) {
                        ColorHolder original = ColorHolder.fromColorInt(sourceImg.getPixelRGBA(x,y));
                        if (original.getA() == 0) {
                            outImg.setPixelRGBA(x,y,0);
                            continue;
                        }
                        float ramp = ((float) palette.closestTo(original))/maxIndex;
                        float value = lowerBound()+(diff*ramp);
                        outImg.setPixelRGBA(x,y,ColorHolder.toColorInt(new ColorHolder(value,value,value,original.getA())));
                    }
                }

                return outImg;
            }
        };
    }
}
