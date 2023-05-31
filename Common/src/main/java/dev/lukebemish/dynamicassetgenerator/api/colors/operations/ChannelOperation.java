package dev.lukebemish.dynamicassetgenerator.api.colors.operations;

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
        return (color >> channel) & 0xFF;
    }
}
