/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.colors.operations;

import dev.lukebemish.dynamicassetgenerator.api.colors.ColorTools;
import net.minecraft.util.FastColor;

/**
 * A collection of common pointwise operations on colors.
 */
public final class ColorOperations {
    private ColorOperations() {}

    /**
     * A pointwise operation that combines the alpha channel of the second color with the RGB channels of the first.
     */
    public static final PointwiseOperation.Binary<Integer> MASK = (i, m, iInBounds, mInBounds) -> {
        if (!mInBounds || !iInBounds)
            return 0;
        int maskAlpha = m & 0xFF000000;
        int oldAlpha = i & 0xFF000000;
        int newAlpha = maskAlpha * oldAlpha / 255;
        return (i & 0xFFFFFF) | newAlpha;
    };

    /**
     * A pointwise operation that overlays all provided colors, using alpha compositing. The first provided color is the
     * top layer, and the last provided color is the bottom layer.
     */
    public static final PointwiseOperation.Any<Integer> OVERLAY = (colors, inBounds) -> {
        if (colors.length == 0)
            return 0;
        int color = 0;
        for (int i = 0; i < colors.length; i++) {
            if (inBounds[i]) {
                color = ColorTools.ARGB32.alphaBlend(color, colors[i]);
            }
        }
        return color;
    };

    /**
     * A pointwise operation that adds all provided colors together, clamping the result to 255.
     */
    public static final PointwiseOperation.Any<Integer> ADD = (colors, inBounds) -> {
        if (colors.length == 0)
            return 0;
        int alpha = 0;
        int red = 0;
        int green = 0;
        int blue = 0;
        for (int i = 0; i < colors.length; i++) {
            if (inBounds[i]) {
                alpha += FastColor.ARGB32.alpha(colors[i]);
                red += FastColor.ARGB32.red(colors[i]);
                green += FastColor.ARGB32.green(colors[i]);
                blue += FastColor.ARGB32.blue(colors[i]);
            }
        }
        return FastColor.ARGB32.color(ColorTools.clamp8(alpha), ColorTools.clamp8(red), ColorTools.clamp8(green), ColorTools.clamp8(blue));
    };

    /**
     * A pointwise operation that multiplies all provided colors together, scaling to a 0-255 range.
     */
    public static final PointwiseOperation.Any<Integer> MULTIPLY = (colors, inBounds) -> {
        if (colors.length == 0)
            return 0;
        float alpha = 255;
        float red = 255;
        float green = 255;
        float blue = 255;
        for (int i = 0; i < colors.length; i++) {
            if (inBounds[i]) {
                alpha *= FastColor.ARGB32.alpha(colors[i]) / 255f;
                red *= FastColor.ARGB32.red(colors[i]) / 255f;
                green *= FastColor.ARGB32.green(colors[i]) / 255f;
                blue *= FastColor.ARGB32.blue(colors[i]) / 255f;
            }
        }
        alpha = Math.round(alpha);
        red = Math.round(red);
        green = Math.round(green);
        blue = Math.round(blue);
        return FastColor.ARGB32.color(ColorTools.clamp8((int) alpha), ColorTools.clamp8((int) red), ColorTools.clamp8((int) green), ColorTools.clamp8((int) blue));
    };

    /**
     * A pointwise operation that inverts the color.
     */
    public static final PointwiseOperation.Unary<Integer> INVERT = (color, inBounds) -> {
        if (!inBounds)
            return 0;
        return ~color;
    };
}
