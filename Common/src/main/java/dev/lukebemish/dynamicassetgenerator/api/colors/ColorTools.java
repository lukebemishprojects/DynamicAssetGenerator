package dev.lukebemish.dynamicassetgenerator.api.colors;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.util.FastColor;

import java.util.Comparator;
import java.util.function.IntUnaryOperator;

/**
 * Various tools for transforming and comparing colors encoded in different ways
 */
@SuppressWarnings("unused")
public final class ColorTools {
    private ColorTools() {}

    /**
     * Tool for comparing colors encoded with the least significant 24 bits representing 3 8-bit channels: red, green,
     * and blue. Any alpha channel is ignored.
     */
    public static final class RGB24 {
        private RGB24() {}

        /**
         * @return the Euclidean distance between two colors in RGB24 format
         */
        public static double distance(int color1, int color2) {
            int dr = FastColor.ARGB32.red(color1) - FastColor.ARGB32.red(color2);
            int dg = FastColor.ARGB32.green(color1) - FastColor.ARGB32.green(color2);
            int db = FastColor.ARGB32.blue(color1) - FastColor.ARGB32.blue(color2);

            return Math.sqrt(dr*dr + dg*dg + db*db);
        }

        /**
         * A comparator that sorts colors by their Euclidean distance from black, with 0xFFFFFF (white) being the
         * largest.
         */
        public static final Comparator<Integer> COMPARATOR = Comparator.comparingInt(i ->
                FastColor.ARGB32.red(i) + FastColor.ARGB32.green(i) + FastColor.ARGB32.blue(i));
    }

    /**
     * Tool for comparing colors encoded with the least significant 32 bits representing 4 8-bit channels: alpha, and
     * the "L", "a", and "b" channels of a CIELAB encoding. Assumes standard illuminant D65.
     */
    public static final class CIELAB32 {
        private static final double MAX_LINEAR = Math.pow(255, 2.2);
        private CIELAB32() {}

        /**
         * A comparator that sorts colors by the lightness of their CIELAB encoding.
         */
        public static final Comparator<Integer> COMPARATOR = Comparator.comparingInt(CIELAB32::lightness);

        /**
         * @return the alpha channel of a color
         */
        public static int alpha(int color) {
            return (color >> 24 & 0xFF);
        }

        /**
         * @return the "L" channel of a color
         */
        public static int lightness(int color) {
            return (color >> 16 & 0xFF);
        }

        /**
         * @return the "a" channel of a color
         */
        public static byte a(int color) {
            return (byte) (color >> 8 & 0xFF);
        }

        /**
         * @return the "b" channel of a color
         */
        public static byte b(int color) {
            return (byte) (color & 0xFF);
        }

        /**
         * @return the provided ARGB32 color in its nearest matching encoding
         */
        public static int fromARGB32(int color) {
            int r = FastColor.ARGB32.red(color);
            int g = FastColor.ARGB32.green(color);
            int b = FastColor.ARGB32.blue(color);

            double lR = linearize(r);
            double lG = linearize(g);
            double lB = linearize(b);

            double x = 0.4124*lR + 0.3576*lG + 0.1805*lB;
            double y = 0.2126*lR + 0.7152*lG + 0.0722*lB;
            double z = 0.0193*lR + 0.1192*lG + 0.9505*lB;

            double l = (116*cielabF(y/YN) - 16);
            double a_ = (500*(cielabF(x/XN) - cielabF(y/YN)));
            double b_ = (200*(cielabF(y/YN) - cielabF(z/ZN)));

            return ((int) Math.round(l) << 16 & 0xFF) | ((int) Math.round(a_) << 8 & 0xFF) | ((int) Math.round(b_) & 0xFF) | (FastColor.ARGB32.alpha(color) << 24);
        }

        /**
         * @return the Euclidean distance between two colors in CIELAB32 format
         */
        public static double distance(int color1, int color2) {
            int dL = lightness(color1) - lightness(color2);
            int da = a(color1) - a(color2);
            int db = b(color1) - b(color2);

            //TODO: test this in production with clustering setup and see if it's sufficient.
            return Math.sqrt(da*da + db*db + dL*dL);
        }

        private static final double XN = 0.95047;
        private static final double YN = 1.00000;
        private static final double ZN = 1.08883;

        private static final double delta = 6d/29d;
        private static final double delta2 = delta*delta;
        private static final double delta3 = delta2*delta;

        private static double cielabF(double t) {
            if (t > delta3)
                return Math.pow(t, 1/3d);
            else
                return t/(3*delta2) + 4/29d;
        }
    }

    /**
     * Tool for making conversions between color spaces more performant. This <em>should</em> be used if it will have a
     * known, short lifespan; hold a comparatively small number of items; and likely have many duplicate conversions.
     * Otherwise, it is likely more performant to simply do conversions as needed.
     */
    public static class ConversionCache {
        private final Int2IntMap cache = new Int2IntOpenHashMap();
        private final IntUnaryOperator converter;

        /**
         * Creates a new cache that will use the provided converter to convert colors.
         * @param converter conversion function to use
         */
        public ConversionCache(IntUnaryOperator converter) {
            this.converter = converter;
        }

        /**
         * @return the provided color after conversion
         */
        public int convert(int color) {
            return cache.computeIfAbsent(color, converter);
        }
    }

    /**
     * @return the provided channel of an sRGB color in linearized form
     */
    public static double linearize(int value) {
        double v = value/255f;
        if (v <= 0.04045)
            return v/12.92;
        else
            return Math.pow((v+0.055)/1.055, 2.4);
    }

    /**
     * @return the provided channel of a linear RGB color in sRGB form
     */
    public static int delinearize(double value) {
        if (value <= 0.0031308)
            return clamp8((int) (value*12.92*255+0.5));
        else
            return clamp8((int) ((1.055*Math.pow(value, 1/2.4)-0.055)*255+0.5));
    }

    /**
     * @return the provided value, clamped to the range [0, 255]
     */
    public static int clamp8(int value) {
        return Math.min(Math.max(value, 0), 255);
    }
}
