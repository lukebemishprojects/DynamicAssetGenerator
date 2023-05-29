package dev.lukebemish.dynamicassetgenerator.api.colors.operations;

/**
 * An operation that can be applied to the entirety of several images by being applied at any given point, to generate
 * data for any given point
 * @param <T> the type of data that this operation generates
 */
public interface PointwiseOperation<T> {
    /**
     * Applies this operation to the given colors and in-bounds flags.
     * @param colors the colors of the images to apply this operation to at the given point
     * @param inBounds whether each image is in-bounds at the given point. At least one image will always be in-bounds
     * @return the data generated at this point
     */
    T apply(int[] colors, boolean[] inBounds);

    /**
     * @return how many images this operation expects to be applied to. Should return -1 if the operation can be applied
     * to any number of images
     */
    int expectedImages();

    /**
     * A pointwise operation that can be applied to a single image.
     */
    interface UnaryPointwiseOperation<T> extends PointwiseOperation<T> {
        T apply(int color, boolean isInBounds);

        @Override
        default int expectedImages() {
            return 1;
        }

        @Override
        default T apply(int[] colors, boolean[] inBounds) {
            if (colors.length != 1 || inBounds.length != 1)
                throw new IllegalArgumentException("Unary operation must have exactly one input image");
            return apply(colors[0], inBounds[0]);
        }
    }
}
