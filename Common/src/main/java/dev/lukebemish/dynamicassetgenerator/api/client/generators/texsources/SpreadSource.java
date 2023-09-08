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
import dev.lukebemish.dynamicassetgenerator.api.colors.ColorTypes;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A {@link TexSource} that maps the colors of another {@link TexSource} to the location of those colors within a
 * palette.
 */
@ApiStatus.Experimental
public final class SpreadSource implements TexSource {
    private static final List<Range> DEFAULT_RANGE = List.of(new Range(0, 255));

    public static final Codec<SpreadSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            TexSource.CODEC.fieldOf("source").forGetter(SpreadSource::getSource),
            Codec.either(Range.CODEC, Range.CODEC.listOf()).xmap(
                    either -> either.map(List::of, Function.identity()),
                    list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)
            ).flatXmap(list -> {
                if (!verifyDisjoint(list)) {
                    return DataResult.error(() -> "Ranges must be disjoint");
                }
                return DataResult.success(list);
            }, DataResult::success).optionalFieldOf("range", DEFAULT_RANGE).forGetter(SpreadSource::getRange)
    ).apply(i, SpreadSource::new));
    private final TexSource source;
    private final List<Range> range;

    private SpreadSource(TexSource source, List<Range> range) {
        this.source = source;
        this.range = range;
    }

    /**
     * Represents a range of integers between 0 and 255, bounded exclusively on the top.
     * @param lowerBound the lower edge of the range; must be between 0 and 255, inclusive
     * @param upperBound the upper edge of the range; must be between 0 and 255, inclusive, and must be larger than the
     *                   lower edge.
     */
    public record Range(int lowerBound, int upperBound) {
        public static final Codec<Range> CODEC = Codec.intRange(0, 255).listOf().flatXmap(list -> {
            if (list.size() != 2) {
                return DataResult.error(() -> "Range must have exactly 2 elements");
            }
            if (list.get(1) <= list.get(0)) {
                return DataResult.error(() -> "Second element of range must be larger than the first");
            }
            return DataResult.success(new Range(list.get(0), list.get(1)));
        }, range -> DataResult.success(List.of(range.lowerBound(), range.upperBound())));
    }

    private static boolean verifyDisjoint(List<Range> ranges) {
        if (ranges.isEmpty()) {
            return false;
        }
        for (int i = 0; i < ranges.size(); i++) {
            if (ranges.get(i).lowerBound() > ranges.get(i).upperBound()) {
                return false;
            }
            for (int j = i + 1; j < ranges.size() && j < i + 2; j++) {
                if (ranges.get(i).upperBound() > ranges.get(j).lowerBound()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int mapToRange(float value, List<Range> ranges) {
        int sum = 0;
        for (Range range : ranges) {
            sum += range.upperBound() - range.lowerBound();
        }
        int current = 0;
        for (Range range : ranges) {
            int rangeSize = range.upperBound() - range.lowerBound();
            if (value < current + rangeSize) {
                float out = range.lowerBound() + (value - current) * rangeSize / sum;
                return ColorTypes.clamp8((int) (out + 0.5));
            }
            current += rangeSize;
        }
        return ranges.get(ranges.size() - 1).upperBound();
    }

    @Override
    public @NonNull Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> source = getSource().getCachedSupplier(data, context);
        if (source == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.getSource().stringify());
            return null;
        }
        return () -> {
            try (NativeImage paletteImage = source.get()) {
                int min = 0xFF;
                int max = 0x00;
                for (int i = 0; i < paletteImage.getWidth(); i++) {
                    for (int j = 0; j < paletteImage.getHeight(); j++) {
                        int color = paletteImage.getPixelRGBA(i, j);
                        int alpha = FastColor.ABGR32.alpha(color);
                        if (alpha != 0) {
                            int value = (FastColor.ABGR32.red(color) + FastColor.ABGR32.green(color) + FastColor.ABGR32.blue(color)) / 3;
                            if (value < min)
                                min = value;
                            if (value > max)
                                max = value;
                        }
                    }
                }
                PointwiseOperation.Unary<Integer> operation = createSpreadingOperation(max, min);

                return ImageUtils.generateScaledImage(operation, List.of(paletteImage));
            }
        };
    }

    private PointwiseOperation.@NonNull Unary<Integer> createSpreadingOperation(int max, int min) {
        return (color, isInBounds) -> {
            int value = (FastColor.ARGB32.red(color) + FastColor.ARGB32.green(color) + FastColor.ARGB32.blue(color)) / 3;
            float stretched = (value - min) * 255f / (max - min);
            int out = mapToRange(stretched, getRange());
            return FastColor.ARGB32.color(FastColor.ARGB32.alpha(color), out, out, out);
        };
    }

    public TexSource getSource() {
        return source;
    }

    public List<Range> getRange() {
        return range;
    }

    public static class Builder {
        private TexSource source;
        private List<Range> range = DEFAULT_RANGE;

        /**
         * Sets the input texture to map the colors of.
         */
        public Builder setSource(TexSource source) {
            this.source = source;
            return this;
        }

        /**
         * Sets the ranges to map the colors to. Defaults to a single range from 0 to 255.
         */
        public Builder setRange(List<Range> range) {
            this.range = range;
            return this;
        }

        public SpreadSource build() {
            Objects.requireNonNull(source);
            return new SpreadSource(source, range);
        }
    }
}
