package dev.lukebemish.dynamicassetgenerator.api.colors.operations;

import dev.lukebemish.dynamicassetgenerator.api.colors.ColorTools;

/**
 * A pointwise operation that overlays images on top of each other via alpha compositing, with the first provided image
 * going on top.
 */
@SuppressWarnings("unused")
public class OverlayOperation implements PointwiseOperation<Integer> {
    public static final OverlayOperation INSTANCE = new OverlayOperation();

    @Override
    public Integer apply(int[] colors, boolean[] inBounds) {
        if (colors.length == 0)
            return 0;
        int color = 0;
        for (int i = 0; i < colors.length; i++) {
            if (inBounds[i]) {
                color = ColorTools.ARGB32.alphaBlend(color, colors[i]);
            }
        }
        return color;
    }

    @Override
    public int expectedImages() {
        return -1;
    }
}
