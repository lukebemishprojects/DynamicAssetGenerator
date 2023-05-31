package dev.lukebemish.dynamicassetgenerator.api.colors.operations;

import dev.lukebemish.dynamicassetgenerator.api.colors.ColorTools;

public class CachedConversionOperation implements PointwiseOperation.Unary<Integer> {
    private final ColorTools.ConversionCache cache;

    public CachedConversionOperation(ColorTools.ConversionCache cache) {
        this.cache = cache;
    }

    @Override
    public Integer apply(int color, boolean isInBounds) {
        return cache.convert(color);
    }
}
