/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.colors;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import org.jetbrains.annotations.Contract;

import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * A collection of color encodings, with tools for converting between them.
 */
public final class ColorTypes {
    private ColorTypes() {}

    /**
     * Colors encoded with the least significant 32 bits representing 4 8-bit channels: alpha, red, green, and blue.
     */
    public static final class ARGB32 extends IntColorType.BlendingIntColorType {
        private ARGB32() {}

        @Contract(pure = true)
        public int red(int color) {
            return first(color);
        }

        @Contract(pure = true)
        public int green(int color) {
            return second(color);
        }

        @Contract(pure = true)
        public int blue(int color) {
            return third(color);
        }

        @Override
        public int toARGB32(int color) {
            return color;
        }

        @Override
        public int fromARGB32(int color) {
            return color;
        }

        /**
         * @return the Euclidean distance between two colors in ARGB32 format, ignoring alpha
         */
        public double distance(int color1, int color2) {
            int dr = red(color1) - red(color2);
            int dg = green(color1) - green(color2);
            int db = blue(color1) - blue(color2);

            return Math.sqrt(dr*dr + dg*dg + db*db);
        }
    }
    public static final ARGB32 ARGB32 = new ARGB32();

    /**
     * Colors encoded with the least significant 32 bits representing 4 8-bit channels: alpha, blue, green, and red.
     * {@link com.mojang.blaze3d.platform.NativeImage}s are encoded this way.
     */
    public static final class ABGR32 extends IntColorType.BlendingIntColorType {
        private ABGR32() {}

        @Contract(pure = true)
        public int red(int color) {
            return third(color);
        }

        @Contract(pure = true)
        public int green(int color) {
            return second(color);
        }

        @Contract(pure = true)
        public int blue(int color) {
            return first(color);
        }

        @Override
        public int toARGB32(int color) {
            return ARGB32.color(alpha(color), red(color), green(color), blue(color));
        }

        @Override
        public int fromARGB32(int color) {
            return color(ARGB32.alpha(color), ARGB32.blue(color), ARGB32.green(color), ARGB32.red(color));
        }
    }
    public static final ABGR32 ABGR32 = new ABGR32();

    /**
     * Colors encoded with the least significant 32 bits representing 4 8-bit channels: alpha, hue, saturation, and
     * lightness.
     */
    public static final class HSL32 extends IntColorType.HueIntColorType {
        private HSL32() {}

        @Contract(pure = true)
        public int saturation(int color) {
            return second(color);
        }

        @Contract(pure = true)
        public int lightness(int color) {
            return third(color);
        }

        @Override
        protected float makeChroma(int color) {
            float s = saturation(color) / 255f;
            float l = lightness(color) / 255f;
            return (1 - Math.abs(2 * l - 1)) * s;
        }

        @Override
        protected float makeMatch(int color, float chroma) {
            return lightness(color) / 255f - chroma / 2;
        }

        @Override
        protected int makeColor(int alpha, int hue, float chroma, float xMin, float xMax) {
            float l = (xMax + xMin) / 2f;
            float v = l + chroma / 2f;
            float s;
            if (l <= 0 || l >= 1) {
                s = 0;
            } else {
                s = (v - l) / Math.min(l, 1 - l);
            }
            return color(alpha, hue, Math.round(s * 0xFF) & 0xFF, Math.round(l * 0xFF) & 0xFF);
        }
    }
    public static final HSL32 HSL32 = new HSL32();

    /**
     * Colors encoded with the least significant 32 bits representing 4 8-bit channels: alpha, hue, saturation, and
     * value.
     */
    public static class HSV32 extends IntColorType.HueIntColorType {
        private HSV32() {}

        @Contract(pure = true)
        public int saturation(int color) {
            return second(color);
        }

        @Contract(pure = true)
        public int value(int color) {
            return third(color);
        }

        @Override
        protected float makeChroma(int color) {
            float s = saturation(color) / 255f;
            float v = value(color) / 255f;
            return s * v;
        }

        @Override
        protected float makeMatch(int color, float chroma) {
            return value(color) / 255f - chroma;
        }

        @Override
        protected int makeColor(int alpha, int hue, float chroma, float xMin, float xMax) {
            float v = ((xMax + xMin) + chroma) / 2f;
            float s = v == 0 ? 0 : chroma / v;
            return color(alpha, hue, Math.round(s * 0xFF) & 0xFF, Math.round(v * 0xFF) & 0xFF);
        }
    }
    public static final HSV32 HSV32 = new HSV32();

    /**
     * Colors encoded with the least significant 32 bits representing 4 8-bit channels: alpha, and the "L", "a",
     * and "b" channels of a CIELAB encoding. Assumes standard illuminant D65.
     */
    public static final class CIELAB32 extends IntColorType {
        private CIELAB32() {}

        @Contract(pure = true)
        public int lightness(int color) {
            return first(color);
        }

        @Contract(pure = true)
        public int a(int color) {
            return (byte) second(color);
        }

        @Contract(pure = true)
        public int b(int color) {
            return (byte) third(color);
        }

        /**
         * @return the Euclidean distance between two colors in CIELAB32 format, ignoring alpha
         */
        public double distance(int color1, int color2) {
            int dL = lightness(color1) - lightness(color2);
            int da = a(color1) - a(color2);
            int db = b(color1) - b(color2);

            return Math.sqrt(da*da + db*db + dL*dL);
        }

        @Override
        public int toARGB32(int color) {
            double x = XN * cielabFinv((lightness(color) + 16)/116d + a(color)/500d);
            double y = YN * cielabFinv((lightness(color) + 16)/116d);
            double z = ZN * cielabFinv((lightness(color) + 16)/116d + b(color)/200d);

            double lR =  3.2406*x + -1.5372*y + -0.4986*z;
            double lG = -0.9689*x +  1.8758*y +  0.0415*z;
            double lB =  0.0557*x + -0.2040*y +  1.0570*z;

            int r = delinearize8(lR);
            int g = delinearize8(lG);
            int b = delinearize8(lB);

            return ARGB32.color(alpha(color), r, g, b);
        }

        @Override
        public int fromARGB32(int color) {
            int r = ARGB32.red(color);
            int g = ARGB32.green(color);
            int b = ARGB32.blue(color);

            double lR = linearize8(r);
            double lG = linearize8(g);
            double lB = linearize8(b);

            double x = 0.4124*lR + 0.3576*lG + 0.1805*lB;
            double y = 0.2126*lR + 0.7152*lG + 0.0722*lB;
            double z = 0.0193*lR + 0.1192*lG + 0.9505*lB;

            double l = (116*cielabF(y/YN) - 16);
            double a_ = (500*(cielabF(x/XN) - cielabF(y/YN)));
            double b_ = (200*(cielabF(y/YN) - cielabF(z/ZN)));

            return color(ARGB32.alpha(color), ((int) Math.round(l) & 0xFF), ((int) Math.round(a_) & 0xFF), ((int) Math.round(b_) & 0xFF));
        }

        private static final double XN = 0.95047;
        private static final double YN = 1.00000;
        private static final double ZN = 1.08883;

        private static final double DELTA = 6d/29d;
        private static final double DELTA_2 = DELTA * DELTA;
        private static final double DELTA_3 = DELTA_2 * DELTA;

        @Contract(pure = true)
        private static double cielabF(double t) {
            if (t > DELTA_3)
                return Math.pow(t, 1/3d);
            else
                return t/(3* DELTA_2) + 4/29d;
        }

        @Contract(pure = true)
        private static double cielabFinv(double t) {
            if (t > DELTA)
                return t*t*t;
            else
                return 3* DELTA_2 *(t - 4/29d);
        }
    }
    public static final CIELAB32 CIELAB32 = new CIELAB32();

    /**
     * Colors encoded with the least significant 64 bits representing 4 16-bit channels: alpha, red, green, and blue.
     */
    public static final class ARGB64 extends LongColorType.BlendingLongColorType {
        private ARGB64() {}

        @Contract(pure = true)
        public int red(long color) {
            return first(color);
        }

        @Contract(pure = true)
        public int green(long color) {
            return second(color);
        }

        @Contract(pure = true)
        public int blue(long color) {
            return third(color);
        }

        @Override
        public long toARGB64(long color) {
            return color;
        }

        @Override
        public long fromARGB64(long color) {
            return color;
        }

        /**
         * @return the Euclidean distance between two colors in ARGB64 format, ignoring alpha
         */
        public double distance(long color1, long color2) {
            int dr = red(color1) - red(color2);
            int dg = green(color1) - green(color2);
            int db = blue(color1) - blue(color2);

            return Math.sqrt(dr*dr + dg*dg + db*db);
        }
    }
    public static final ARGB64 ARGB64 = new ARGB64();

    /**
     * Colors encoded with the least significant 64 bits representing 4 16-bit channels: alpha, blue, green, and red.
     */
    public static final class ABGR64 extends LongColorType.BlendingLongColorType {
        private ABGR64() {}

        @Contract(pure = true)
        public int red(long color) {
            return third(color);
        }

        @Contract(pure = true)
        public int green(long color) {
            return second(color);
        }

        @Contract(pure = true)
        public int blue(long color) {
            return first(color);
        }

        @Override
        public long toARGB64(long color) {
            return ARGB64.color(alpha(color), red(color), green(color), blue(color));
        }

        @Override
        public long fromARGB64(long color) {
            return color(ARGB64.alpha(color), ARGB64.red(color), ARGB64.green(color), ARGB64.blue(color));
        }
    }
    public static final ABGR64 ABGR64 = new ABGR64();

    /**
     * Colors encoded with the least significant 64 bits representing 4 16-bit channels: alpha, hue, saturation, and
     * lightness.
     */
    public static final class HSL64 extends LongColorType.HueLongColorType {
        private HSL64() {}

        @Contract(pure = true)
        public int saturation(long color) {
            return second(color);
        }

        @Contract(pure = true)
        public int lightness(long color) {
            return third(color);
        }

        @Override
        protected float makeChroma(long color) {
            float s = saturation(color) / 0xFFFFp0f;
            float l = lightness(color) / 0xFFFFp0f;
            return (1 - Math.abs(2 * l - 1)) * s;
        }

        @Override
        protected float makeMatch(long color, float chroma) {
            return lightness(color) / 0xFFFFp0f - chroma / 2;
        }

        @Override
        protected long makeColor(int alpha, int hue, float chroma, float xMin, float xMax) {
            float l = (xMax + xMin) / 2f;
            float v = l + chroma / 2f;
            float s;
            if (l <= 0 || l >= 1) {
                s = 0;
            } else {
                s = (v - l) / Math.min(l, 1 - l);
            }
            return color(alpha, hue, Math.round(s * 0xFFFF) & 0xFFFF, Math.round(l * 0xFFFF) & 0xFFFF);
        }
    }
    public static final HSL64 HSL64 = new HSL64();

    /**
     * Colors encoded with the least significant 64 bits representing 4 16-bit channels: alpha, hue, saturation, and
     * value.
     */
    public static class HSV64 extends LongColorType.HueLongColorType {
        private HSV64() {}

        @Contract(pure = true)
        public int saturation(long color) {
            return second(color);
        }

        @Contract(pure = true)
        public int value(long color) {
            return third(color);
        }

        @Override
        protected float makeChroma(long color) {
            float s = saturation(color) / 0xFFFFp0f;
            float v = value(color) / 0xFFFFp0f;
            return s * v;
        }

        @Override
        protected float makeMatch(long color, float chroma) {
            return value(color) / 0xFFFFp0f - chroma;
        }

        @Override
        protected long makeColor(int alpha, int hue, float chroma, float xMin, float xMax) {
            float v = ((xMax + xMin) + chroma) / 2f;
            float s = v == 0 ? 0 : chroma / v;
            return color(alpha, hue, Math.round(s * 0xFFFF) & 0xFFFF, Math.round(v * 0xFFFF) & 0xFFFF);
        }
    }
    public static final HSV64 HSV64 = new HSV64();

    /**
     * Colors encoded with the least significant 64 bits representing 4 16-bit channels: alpha, and the "L", "a",
     * and "b" channels of a CIELAB encoding. Assumes standard illuminant D65. Coordinates are scaled by a factor of
     * 255 before encoding, so the float version provided by {@link #lightness}, {@link #a}, and {@link #b} should be
     * used for calculations.
     */
    public static final class CIELAB64 extends LongColorType {
        private CIELAB64() {}

        @Contract(pure = true)
        public float lightness(long color) {
            return first(color) / 255f;
        }

        @Contract(pure = true)
        public float a(long color) {
            return (short) second(color) / 255f;
        }

        @Contract(pure = true)
        public float b(long color) {
            return (short) third(color) / 255f;
        }

        /**
         * @return the Euclidean distance between two colors in CIELAB32 format, ignoring alpha
         */
        public double distance(long color1, long color2) {
            float dL = lightness(color1) - lightness(color2);
            float da = a(color1) - a(color2);
            float db = b(color1) - b(color2);

            return Math.sqrt(da*da + db*db + dL*dL);
        }

        @Override
        public long toARGB64(long color) {
            double x = XN * cielabFinv((lightness(color) + 16)/116d + a(color)/500d);
            double y = YN * cielabFinv((lightness(color) + 16)/116d);
            double z = ZN * cielabFinv((lightness(color) + 16)/116d + b(color)/200d);

            double lR =  3.2406*x + -1.5372*y + -0.4986*z;
            double lG = -0.9689*x +  1.8758*y +  0.0415*z;
            double lB =  0.0557*x + -0.2040*y +  1.0570*z;

            int r = delinearize16(lR);
            int g = delinearize16(lG);
            int b = delinearize16(lB);

            return ARGB64.color(alpha(color), r, g, b);
        }

        @Override
        public long fromARGB64(long color) {
            int r = ARGB64.red(color);
            int g = ARGB64.green(color);
            int b = ARGB64.blue(color);

            double lR = linearize16(r);
            double lG = linearize16(g);
            double lB = linearize16(b);

            double x = 0.4124*lR + 0.3576*lG + 0.1805*lB;
            double y = 0.2126*lR + 0.7152*lG + 0.0722*lB;
            double z = 0.0193*lR + 0.1192*lG + 0.9505*lB;

            double l = (116*cielabF(y/YN) - 16);
            double a_ = (500*(cielabF(x/XN) - cielabF(y/YN)));
            double b_ = (200*(cielabF(y/YN) - cielabF(z/ZN)));

            return color(ARGB64.alpha(color), ((int) Math.round(l*255) & 0xFFFF), ((int) Math.round(a_*255) & 0xFFFF), ((int) Math.round(b_*255) & 0xFFFF));
        }

        private static final double XN = 0.95047;
        private static final double YN = 1.00000;
        private static final double ZN = 1.08883;

        private static final double DELTA = 6d/29d;
        private static final double DELTA_2 = DELTA * DELTA;
        private static final double DELTA_3 = DELTA_2 * DELTA;

        @Contract(pure = true)
        private static double cielabF(double t) {
            if (t > DELTA_3)
                return Math.pow(t, 1/3d);
            else
                return t/(3* DELTA_2) + 4/29d;
        }

        @Contract(pure = true)
        private static double cielabFinv(double t) {
            if (t > DELTA)
                return t*t*t;
            else
                return 3* DELTA_2 *(t - 4/29d);
        }
    }
    public static final CIELAB64 CIELAB64 = new CIELAB64();

    /**
     * @return the provided value, clamped to the range [0x00, 0xFF]
     */
    @Contract(pure = true)
    public static int clamp8(int value) {
        return Math.min(Math.max(value, 0), 0xFF);
    }

    /**
     * @return the provided value, clamped to the range [0x0000, 0xFFFF]
     */
    @Contract(pure = true)
    public static int clamp16(int value) {
        return Math.min(Math.max(value, 0), 0xFFFF);
    }

    /**
     * @return the provided 8-bit channel of an sRGB color in linearized form
     */
    @Contract(pure = true)
    public static double linearize8(int value) {
        double v = value/255f;
        if (v <= 0.04045)
            return v/12.92;
        else
            return Math.pow((v+0.055)/1.055, 2.4);
    }

    /**
     * @return the provided 8-bit channel of a linear RGB color in sRGB form
     */
    @Contract(pure = true)
    public static int delinearize8(double value) {
        if (value <= 0.0031308)
            return clamp8((int) (value*12.92*255+0.5));
        else
            return clamp8((int) ((1.055*Math.pow(value, 1/2.4)-0.055)*255+0.5));
    }

    /**
     * @return the provided 16-bit channel of an sRGB color in linearized form
     */
    @Contract(pure = true)
    public static double linearize16(int value) {
        double v = value/0xFFFFp0f;
        if (v <= 0.04045)
            return v/12.92;
        else
            return Math.pow((v+0.055)/1.055, 2.4);
    }

    /**
     * @return the provided 16-bit channel of a linear RGB color in sRGB form
     */
    @Contract(pure = true)
    public static int delinearize16(double value) {
        if (value <= 0.0031308)
            return clamp16((int) (value*12.92*0xFFFF+0.5));
        else
            return clamp16((int) ((1.055*Math.pow(value, 1/2.4)-0.055)*0xFFFF+0.5));
    }

    /**
     * @return the closest 32-bit approximation of the provided 64-bit color
     */
    @Contract(pure = true)
    public static int demote(long color) {
        int a = (int) (color >> (48+8)) & 0xFF;
        int c1 = (int) (color >> (32+8)) & 0xFF;
        int c2 = (int) (color >> (16+8)) & 0xFF;
        int c3 = (int) (color >> 8) & 0xFF;
        return a << 24 | c1 << 16 | c2 << 8 | c3;
    }

    /**
     * @return a 64-bit approximation of the provided 32-bit color
     */
    @Contract(pure = true)
    public static long promote(int color) {
        long a = color >> 16 & 0xFF00;
        long c1 = color >> 8 & 0xFF00;
        long c2 = color & 0xFF00;
        int c3 = color << 8 & 0xFF00;
        return a << 48 | c1 << 32 | c2 << 16 | c3;
    }

    /**
     * Tool for making conversions between 32-bit color spaces more performant. This <em>should</em> be used if it will
     * have a known, short lifespan; hold a comparatively small number of items; and likely have many duplicate
     * conversions. Otherwise, it is likely more performant to simply do conversions as needed.
     */
    public static class ConversionCache32 {
        private final Int2IntMap cache = new Int2IntOpenHashMap();
        private final IntUnaryOperator converter;

        /**
         * Creates a new cache that will use the provided converter to convert colors.
         * @param converter conversion function to use
         */
        public ConversionCache32(IntUnaryOperator converter) {
            this.converter = converter;
        }

        /**
         * @return the provided color after conversion. This operation is thread-safe.
         */
        public int convert(int color) {
            synchronized (cache) {
                if (cache.containsKey(color))
                    return cache.get(color);
            }
            int result = converter.applyAsInt(color);
            synchronized (cache) {
                if (!cache.containsKey(color))
                    cache.put(color, result);
            }
            return result;
        }
    }

    /**
     * Tool for making conversions between 64-bit color spaces more performant. This <em>should</em> be used if it will
     * have a known, short lifespan; hold a comparatively small number of items; and likely have many duplicate
     * conversions. Otherwise, it is likely more performant to simply do conversions as needed.
     */
    public static class ConversionCache64 {
        private final Long2LongMap cache = new Long2LongOpenHashMap();
        private final LongUnaryOperator converter;

        /**
         * Creates a new cache that will use the provided converter to convert colors.
         * @param converter conversion function to use
         */
        public ConversionCache64(LongUnaryOperator converter) {
            this.converter = converter;
        }

        /**
         * @return the provided color after conversion. This operation is thread-safe.
         */
        public long convert(long color) {
            synchronized (cache) {
                if (cache.containsKey(color))
                    return cache.get(color);
            }
            long result = converter.applyAsLong(color);
            synchronized (cache) {
                if (!cache.containsKey(color))
                    cache.put(color, result);
            }
            return result;
        }
    }
}
