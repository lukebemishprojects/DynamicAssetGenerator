/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.colors;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.util.FastColor;

import java.util.Comparator;
import java.util.function.IntUnaryOperator;

/**
 * Various tools for transforming and comparing colors encoded in different ways
 */
public final class ColorTools {
    private ColorTools() {}

    /**
     * Tool for comparing colors encoded with the least significant 32 bits representing 4 8-bit channels: alpha, red,
     * green, and blue.
     */
    public static final class ARGB32 {
        private ARGB32() {}

        /**
         * @return the Euclidean distance between two colors, ignoring alpha
         */
        public static double distance(int color1, int color2) {
            int dr = FastColor.ARGB32.red(color1) - FastColor.ARGB32.red(color2);
            int dg = FastColor.ARGB32.green(color1) - FastColor.ARGB32.green(color2);
            int db = FastColor.ARGB32.blue(color1) - FastColor.ARGB32.blue(color2);

            return Math.sqrt(dr*dr + dg*dg + db*db);
        }

        /**
         * A comparator that sorts colors by their Euclidean distance from black, ignoring alpha, with 0xFFFFFF (white)
         * being the largest.
         */
        public static final Comparator<Integer> COMPARATOR = Comparator.comparingInt(i ->
                FastColor.ARGB32.red(i) + FastColor.ARGB32.green(i) + FastColor.ARGB32.blue(i));

        /**
         * Calculates a composite color from two colors to layer
         * @param over the color to layer over the other
         * @param under the color to layer under the other
         * @return a composite color calculated by alpha blending
         */
        public static int alphaBlend(int over, int under) {
            int aOver = FastColor.ARGB32.alpha(over);
            int aUnder = FastColor.ARGB32.alpha(under);
            int a = aOver + (aUnder * (255 - aOver) / 255);
            if (a == 0) return 0;
            int r = (FastColor.ARGB32.red(over) * aOver + FastColor.ARGB32.red(under) * aUnder * (255 - aOver) / 255) / a;
            int g = (FastColor.ARGB32.green(over) * aOver + FastColor.ARGB32.green(under) * aUnder * (255 - aOver) / 255) / a;
            int b = (FastColor.ARGB32.blue(over) * aOver + FastColor.ARGB32.blue(under) * aUnder * (255 - aOver) / 255) / a;
            return FastColor.ARGB32.color(a, r, g, b);
        }

        /**
         * @return the provided ABGR32 encoded color in ARGB32
         */
        public static int fromABGR32(int color) {
            return FastColor.ARGB32.color(
                    FastColor.ABGR32.alpha(color),
                    FastColor.ABGR32.red(color),
                    FastColor.ABGR32.green(color),
                    FastColor.ABGR32.blue(color)
            );
        }
    }

    /**
     * Tool for comparing colors encoded with the least significant 32 bits representing 4 8-bit channels: alpha, blue,
     * green, and red. {@link com.mojang.blaze3d.platform.NativeImage}s are encoded this way.
     */
    public static final class ABGR32 {

        /**
         * @return the provided ARGB32 encoded color in ABGR32
         */
        public static int fromARGB32(int color) {
            return FastColor.ABGR32.color(
                    FastColor.ARGB32.alpha(color),
                    FastColor.ARGB32.blue(color),
                    FastColor.ARGB32.green(color),
                    FastColor.ARGB32.red(color)
            );
        }
    }

    /**
     * Tool for comparing colors encoded with the least significant 32 bits representing 4 8-bit channels: alpha, and
     * the "L", "a", and "b" channels of a CIELAB encoding. Assumes standard illuminant D65.
     */
    public static final class CIELAB32 {
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

            return (((int) Math.round(l) & 0xFF) << 16) | (((int) Math.round(a_) & 0xFF) << 8) | ((int) Math.round(b_) & 0xFF) | (FastColor.ARGB32.alpha(color) << 24);
        }

        /**
         * @return the Euclidean distance between two colors in CIELAB32 format
         */
        public static double distance(int color1, int color2) {
            int dL = lightness(color1) - lightness(color2);
            int da = a(color1) - a(color2);
            int db = b(color1) - b(color2);

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
     * Tool for comparing colors encoded with the least significant 32 bits representing 4 8-bit channels: alpha, hue,
     * saturation, and lightness.
     */
    public static class HSL32 {

        /**
         * A comparator that sorts colors by the lightness of their HSL24 encoding.
         */
        public static final Comparator<Integer> COMPARATOR = Comparator.comparingInt(HSL32::lightness);

        /**
         * @return the alpha channel of a color
         */
        public static int alpha(int color) {
            return (color >> 24) & 0xFF;
        }

        /**
         * @return the hue channel of a color
         */
        public static int hue(int color) {
            return (color >> 16) & 0xFF;
        }

        /**
         * @return the saturation channel of a color
         */
        public static int saturation(int color) {
            return (color >> 8) & 0xFF;
        }

        /**
         * @return the lightness channel of a color
         */
        public static int lightness(int color) {
            return color & 0xFF;
        }

        /**
         * @return the provided ARGB32 color in its nearest matching encoding
         */
        public static int fromARGB32(int color) {
            int r = FastColor.ARGB32.red(color);
            int g = FastColor.ARGB32.green(color);
            int b = FastColor.ARGB32.blue(color);

            int max = Math.max(r, Math.max(g, b));
            int min = Math.min(r, Math.min(g, b));

            int h, s, l;

            l = (max + min) / 2;
            if (max == min) {
                h = s = 0;
            } else {
                int diff = max-min;
                s = l >= 0x80 ? diff*0xFF / (2*0xFF - max - min) : diff*0xFF / (max + min);
                int m = max==r ? 0 : max==g ? 1 : 2;
                h = switch (m) {
                    case 0 -> (g - b)*0xFF / diff + (g < b ? 6*0xFF : 0);
                    case 1 -> (b - r)*0xFF / diff + 2*0xFF;
                    default -> (r - g)*0xFF / diff + 4*0xFF;
                };
                h/=6;
            }
            return ((h & 0xFF) << 16) | ((s & 0xFF) << 8) | l | (FastColor.ARGB32.alpha(color) << 24);
        }

        /**
         * @return a color from the given channels
         */
        public static int color(int hue, int lightness, int saturation) {
            return ((hue & 0xFF) << 16) | ((saturation & 0xFF) << 8) | (lightness & 0xFF);
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
         * @return the provided color after conversion. This operation is thread-safe.
         */
        public int convert(int color) {
            if (cache.containsKey(color))
                return cache.get(color);
            synchronized (cache) {
                if (cache.containsKey(color))
                    return cache.get(color);
                int result = converter.applyAsInt(color);
                cache.put(color, result);
                return result;
            }
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
