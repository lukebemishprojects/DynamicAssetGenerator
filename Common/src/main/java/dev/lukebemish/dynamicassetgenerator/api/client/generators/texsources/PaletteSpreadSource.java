/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import java.util.List;
import java.util.function.Function;

import com.mojang.blaze3d.platform.NativeImage;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.NativeImageHelper;
import dev.lukebemish.dynamicassetgenerator.impl.client.palette.ColorHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.palette.Palette;
import net.minecraft.server.packs.resources.IoSupplier;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Experimental
public record PaletteSpreadSource(TexSource source, float paletteCutoff, List<Range> range) implements TexSource {
    public static final Codec<PaletteSpreadSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            TexSource.CODEC.fieldOf("source").forGetter(PaletteSpreadSource::source),
            Codec.FLOAT.optionalFieldOf("palette_cutoff", Palette.DEFAULT_CUTOFF).forGetter(PaletteSpreadSource::paletteCutoff),
            Codec.either(Range.CODEC, Range.CODEC.listOf()).xmap(
                    either -> either.map(List::of, Function.identity()),
                    list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)
            ).flatXmap(list -> {
                if (!verifyDisjoint(list)) {
                    return DataResult.error(() -> "Ranges must be disjoint");
                }
                return DataResult.success(list);
            }, DataResult::success).optionalFieldOf("range", List.of(new Range(0.0f,1.0f))).forGetter(PaletteSpreadSource::range)
    ).apply(i,PaletteSpreadSource::new));

    public record Range(float lowerBound, float upperBound) {
        public static final Codec<Range> CODEC = Codec.floatRange(0.0f,1.0f).listOf().flatXmap(list -> {
            if (list.size() != 2) {
                return DataResult.error(() -> "Range must have exactly 2 elements");
            }
            return DataResult.success(new Range(list.get(0), list.get(1)));
        }, range -> DataResult.success(List.of(range.lowerBound(), range.upperBound())));
    }

    static boolean verifyDisjoint(List<Range> ranges) {
        if (ranges.size() == 0) {
            return false;
        }
        for (int i = 0; i < ranges.size(); i++) {
            if (ranges.get(i).lowerBound() > ranges.get(i).upperBound()) {
                return false;
            }
            for (int j = i+1; j < ranges.size() && j < i+2; j++) {
                if (ranges.get(i).upperBound() > ranges.get(j).lowerBound()) {
                    return false;
                }
            }
        }
        return true;
    }

    static float mapToRange(float value, List<Range> ranges) {
        float sum = 0.0f;
        for (Range range : ranges) {
            sum += range.upperBound()-range.lowerBound();
        }
        float current = 0.0f;
        for (Range range : ranges) {
            float rangeSize = range.upperBound()-range.lowerBound();
            if (value < current+rangeSize) {
                return range.lowerBound()+(value-current)*(rangeSize/sum);
            }
            current += rangeSize;
        }
        return ranges.get(ranges.size()-1).upperBound();
    }

    @Override
    public Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> source = source().getSupplier(data, context);
        if (source == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.source());
            return null;
        }
        return () -> {
            try (NativeImage sourceImg = source.get()) {
                Palette palette = Palette.extractPalette(sourceImg, 0, paletteCutoff());
                NativeImage outImg = NativeImageHelper.of(NativeImage.Format.RGBA, sourceImg.getWidth(), sourceImg.getHeight(), false);

                int maxIndex = palette.getSize()-1;

                for (int x = 0; x < sourceImg.getWidth(); x++) {
                    for (int y = 0; y < sourceImg.getHeight(); y++) {
                        ColorHolder original = ColorHolder.fromColorInt(sourceImg.getPixelRGBA(x,y));
                        if (original.getA() == 0) {
                            outImg.setPixelRGBA(x,y,0);
                            continue;
                        }
                        float ramp = ((float) palette.closestTo(original))/maxIndex;
                        float value = mapToRange(ramp, range());
                        outImg.setPixelRGBA(x,y,ColorHolder.toColorInt(new ColorHolder(value,value,value,original.getA())));
                    }
                }

                return outImg;
            }
        };
    }
}
