package dev.lukebemish.dynamicassetgenerator.api.colors.operations;

import dev.lukebemish.dynamicassetgenerator.api.colors.ColorTools;
import net.minecraft.util.FastColor;

public final class Operations {
    private Operations() {}

    public static final PointwiseOperation.BinaryPointwiseOperation<Integer> MASK = (i, m, iInBounds, mInBounds) -> {
        if (!mInBounds || !iInBounds)
            return 0;
        int newAlpha = (i >> 24 & 0xFF) << 24;
        return (i & 0xFFFFFF) | newAlpha;
    };

    public static final PointwiseOperation.ManyPointwiseOperation<Integer> OVERLAY = (colors, inBounds) -> {
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

    public static final PointwiseOperation.ManyPointwiseOperation<Integer> ADD = (colors, inBounds) -> {
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

    public static final PointwiseOperation.ManyPointwiseOperation<Integer> MULTIPLY = (colors, inBounds) -> {
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

    public static final PointwiseOperation.UnaryPointwiseOperation<Integer> INVERT = (color, inBounds) -> {
        if (!inBounds)
            return 0;
        return ~color;
    };
}
