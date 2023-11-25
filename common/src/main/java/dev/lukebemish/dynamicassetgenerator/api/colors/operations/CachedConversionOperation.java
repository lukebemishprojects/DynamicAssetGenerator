/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.colors.operations;

import dev.lukebemish.dynamicassetgenerator.api.colors.ColorTypes;

/**
 * A {@link PointwiseOperation.Unary} that uses a {@link ColorTypes.ConversionCache32} to convert colors.
 */
public class CachedConversionOperation implements PointwiseOperation.Unary<Integer> {
    private final ColorTypes.ConversionCache32 cache;

    public CachedConversionOperation(ColorTypes.ConversionCache32 cache) {
        this.cache = cache;
    }

    @Override
    public Integer apply(int color, boolean isInBounds) {
        return cache.convert(color);
    }
}
