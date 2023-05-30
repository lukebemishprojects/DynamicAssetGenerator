package dev.lukebemish.dynamicassetgenerator.api.client.image;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import dev.lukebemish.dynamicassetgenerator.api.colors.ColorTools;
import dev.lukebemish.dynamicassetgenerator.api.colors.Palette;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;
import dev.lukebemish.dynamicassetgenerator.impl.util.Maath;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.List;

/**
 * A series of utilities for working with images and colors.
 */
@SuppressWarnings("unused")
public final class ImageUtils {
    private ImageUtils() {}

    /**
     * @return a palette with a given cutoff of colors from an image
     */
    public static Palette getPalette(NativeImage image, double cutoff) {
        Palette palette = new Palette(cutoff);
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                palette.add(ColorTools.ARGB32.fromABGR32(image.getPixelRGBA(i, j)));
            }
        }
        return palette;
    }

    /**
     * @return the color at a given position in an image, or a default value if the position is out of bounds
     */
    public static int safeGetPixelABGR(NativeImage image, int x, int y, int def) {
        if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
            return def;
        }
        return image.getPixelRGBA(x, y);
    }

    /**
     * @return the color at a given position in an image, or 0x00000000 if the position is out of bounds
     */
    public static int safeGetPixelABGR(NativeImage image, int x, int y) {
        return safeGetPixelABGR(image, x, y, 0);
    }

    /**
     * Sets the color at a given position in an image, if the position is in bounds.
     * @return whether the position was in bounds
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean safeSetPixelABGR(NativeImage image, int x, int y, int color) {
        if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
            return false;
        }
        image.setPixelRGBA(x, y, color);
        return true;
    }

    /**
     * @return the color at a given position in an image, or a default value if the position is out of bounds, in ARGB32
     * encoding
     */
    public static int safeGetPixelARGB(NativeImage image, int x, int y, int def) {
        if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
            return def;
        }
        return ColorTools.ARGB32.fromABGR32(image.getPixelRGBA(x, y));
    }

    /**
     * @return the color at a given position in an image, or 0x00000000 if the position is out of bounds, in ARGB32
     * encoding
     */
    public static int safeGetPixelARGB(NativeImage image, int x, int y) {
        return safeGetPixelABGR(image, x, y, 0);
    }

    /**
     * Sets the color at a given position in an image, if the position is in bounds.
     * @return whether the position was in bounds, in ARGB32 encoding
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean safeSetPixelARGB(NativeImage image, int x, int y, int color) {
        if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
            return false;
        }
        image.setPixelRGBA(x, y, ColorTools.ABGR32.fromARGB32(color));
        return true;
    }

    /**
     * Calculates the dimensions of an image that can contain all the supplied images, scaled so that their widths are
     * all equal.
     */
    public static Pair<Integer, Integer> calculateScaledSize(List<NativeImage> images) {
        IntList widths = new IntArrayList(images.size());
        IntList heights = new IntArrayList(images.size());
        for (NativeImage image : images) {
            widths.add(image.getWidth());
            heights.add(image.getHeight());
        }
        int maxWidth = Maath.lcm(widths);
        int maxHeight = 0;
        for (int i = 0; i < images.size(); i++) {
            int scaledHeight = maxWidth / widths.getInt(i) * heights.getInt(i);
            if (scaledHeight > maxHeight) {
                maxHeight = scaledHeight;
            }
        }
        return Pair.of(maxWidth, maxHeight);
    }

    /**
     * Applies a pointwise operation to a list of images, scaled so that their widths are all equal, and feeds the
     * results and their positions to a consumer.
     * @param pointwiseOperation the operation to apply
     * @param consumer the consumer to feed the results to - accepts the x and y coordinates of the pixel, and the data
     *                 generated by the operation
     * @param images the images to apply the operation to. The number of images must match the number expected by the
     *               operation, or the operation must expect any number of images
     * @param <T> the type of data generated by the operation
     */
    public static <T> void applyScaledOperation(PointwiseOperation<T> pointwiseOperation, TriConsumer<Integer, Integer, T> consumer, List<NativeImage> images) {
        if (pointwiseOperation.expectedImages() != images.size() && pointwiseOperation.expectedImages() != -1)
            throw new IllegalArgumentException("Expected " + pointwiseOperation.expectedImages() + " images, got " + images.size());
        Pair<Integer, Integer> scaledSize = calculateScaledSize(images);
        int width = scaledSize.getFirst();
        int height = scaledSize.getSecond();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int[] colors = new int[pointwiseOperation.expectedImages()];
                boolean[] inBounds = new boolean[pointwiseOperation.expectedImages()];
                for (int k = 0; k < pointwiseOperation.expectedImages(); k++) {
                    int x = i * images.get(k).getWidth() / width;
                    int y = j * images.get(k).getHeight() / height;
                    colors[k] = safeGetPixelARGB(images.get(k), x, y, 0);
                    inBounds[k] = (x >= 0 && x < images.get(k).getWidth() && y >= 0 && y < images.get(k).getHeight());
                }
                T t = pointwiseOperation.apply(colors, inBounds);
                consumer.accept(i, j, t);
            }
        }
    }

    /**
     * Generates an image by applying a pointwise operation to a list of images, scaled so that their widths are all
     * equal.
     * @param pointwiseOperation the operation to apply
     * @param images the images to apply the operation to. The number of images must match the number expected by the
     *               operation, or the operation must expect any number of images
     * @return the generated image
     */
    public static NativeImage generateScaledImage(PointwiseOperation<Integer> pointwiseOperation, List<NativeImage> images) {
        Pair<Integer, Integer> scaledSize = calculateScaledSize(images);
        int width = scaledSize.getFirst();
        int height = scaledSize.getSecond();
        NativeImage out = new NativeImage(width, height, false);
        applyScaledOperation(pointwiseOperation, (x, y, color) -> safeSetPixelARGB(out, x, y, color), images);
        return out;
    }
}
