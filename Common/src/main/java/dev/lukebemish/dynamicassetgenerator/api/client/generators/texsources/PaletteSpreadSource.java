/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.api.client.image.ImageUtils;
import dev.lukebemish.dynamicassetgenerator.api.colors.Palette;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

@ApiStatus.Experimental
public record PaletteSpreadSource(TexSource source, float paletteCutoff, List<Range> range) implements TexSource {
    public static final Codec<PaletteSpreadSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            TexSource.CODEC.fieldOf("source").forGetter(PaletteSpreadSource::source),
            Codec.FLOAT.optionalFieldOf("palette_cutoff", 2f).forGetter(PaletteSpreadSource::paletteCutoff),
            Codec.either(Range.CODEC, Range.CODEC.listOf()).xmap(
                    either -> either.map(List::of, Function.identity()),
                    list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)
            ).flatXmap(list -> {
                if (!verifyDisjoint(list)) {
                    return DataResult.error(() -> "Ranges must be disjoint");
                }
                return DataResult.success(list);
            }, DataResult::success).optionalFieldOf("range", List.of(new Range(0,255))).forGetter(PaletteSpreadSource::range)
    ).apply(i,PaletteSpreadSource::new));

    public record Range(int lowerBound, int upperBound) {
        public static final Codec<Range> CODEC = Codec.intRange(0,255).listOf().flatXmap(list -> {
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

    static int mapToRange(int value, List<Range> ranges) {
        int sum = 0;
        for (Range range : ranges) {
            sum += range.upperBound()-range.lowerBound();
        }
        int current = 0;
        for (Range range : ranges) {
            int rangeSize = range.upperBound()-range.lowerBound();
            if (value < current+rangeSize) {
                return range.lowerBound()+(value-current)*rangeSize/sum;
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
                //TODO: Palette cutoff, still...
                Palette palette = ImageUtils.getPalette(sourceImg, paletteCutoff());
                PointwiseOperation.Unary<Integer> operation = (c, i) -> {
                    int sample = palette.getSample(c);
                    sample = mapToRange(sample, range());
                    return FastColor.ARGB32.color(0xFF, sample, sample, sample);
                };

                return ImageUtils.generateScaledImage(operation, List.of(sourceImg));
            }
        };
    }
}
