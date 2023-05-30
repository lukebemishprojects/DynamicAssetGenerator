package dev.lukebemish.dynamicassetgenerator.api.colors.operations;

import dev.lukebemish.dynamicassetgenerator.api.colors.Palette;
import net.minecraft.util.FastColor;

/**
 * A single-image pointwise operation that maps from a color to a palette sample number.
 */
@SuppressWarnings("unused")
public class ColorToPaletteOperation implements PointwiseOperation.Unary<Integer> {
    private final Palette palette;

    public ColorToPaletteOperation(Palette palette) {
        this.palette = palette;
    }

    public Palette getPalette() {
        return palette;
    }

    @Override
    public Integer apply(int color, boolean isInBounds) {
        if (!isInBounds)
            return 0;
        if ((color & 0xFF000000) == 0)
            return 0;
        int value = palette.getSample(color);
        return FastColor.ARGB32.color(FastColor.ARGB32.alpha(color), value, value, value);
    }
}
