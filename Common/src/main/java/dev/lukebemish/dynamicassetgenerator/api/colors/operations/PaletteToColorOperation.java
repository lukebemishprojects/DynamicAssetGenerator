package dev.lukebemish.dynamicassetgenerator.api.colors.operations;

import dev.lukebemish.dynamicassetgenerator.api.colors.Palette;
import net.minecraft.util.FastColor;


/**
 * A single-image pointwise operation that maps from a palette sample number to a color.
 */
@SuppressWarnings("unused")
public class PaletteToColorOperation implements PointwiseOperation.Unary<Integer> {
    private final Palette palette;

    public PaletteToColorOperation(Palette palette) {
        this.palette = palette;
    }

    public Palette getPalette() {
        return palette;
    }

    @Override
    public Integer apply(int color, boolean isInBounds) {
        if (!isInBounds)
            return 0;
        int value = ((color >> 16 & 0xFF) + (color >> 8 & 0xFF) + (color & 0xFF)) / 3;
        return (palette.getColor(value) & 0xFFFFFF) | (FastColor.ARGB32.alpha(color) << 24);
    }
}
