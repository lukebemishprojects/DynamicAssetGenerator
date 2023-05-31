package dev.lukebemish.dynamicassetgenerator.api.colors.operations;

import net.minecraft.util.FastColor;

/**
 * A pointwise operation that extracts a single channel from a color.
 */
public class ChannelOperation implements PointwiseOperation.Unary<Integer> {
    private final int channel;

    public ChannelOperation(int channel) {
        this.channel = channel*8;
    }

    @Override
    public Integer apply(int color, boolean isInBounds) {
        int value = (color >> channel) & 0xFF;
        return FastColor.ARGB32.color(value, 0xFF, 0xFF, 0xFF);
    }
}
