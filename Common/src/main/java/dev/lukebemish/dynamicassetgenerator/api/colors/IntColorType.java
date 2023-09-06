/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.colors;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.Contract;

/**
 * A type of color that has 4 8-bit channels, the first of which encodes the alpha value.
 */
public abstract class IntColorType {
    @Contract(pure = true)
    public final int alpha(int color) {
        return color >> 24 & 0xFF;
    }

    @Contract(pure = true)
    protected final int first(int color) {
        return color >> 16 & 0xFF;
    }

    @Contract(pure = true)
    protected final int second(int color) {
        return color >> 8 & 0xFF;
    }

    @Contract(pure = true)
    protected final int third(int color) {
        return color & 0xFF;
    }

    /**
     * @return the color, encoded as {@link ColorTypes.ARGB32}
     */
    @Contract(pure = true)
    abstract public int toARGB32(int color);

    /**
     * @return the provided {@link ColorTypes.ARGB32} color, encoded as this color type
     */
    @Contract(pure = true)
    abstract public int fromARGB32(int color);

    /**
     * Constructs a new color from channel values between 0 and 255.
     * @param alpha the alpha channel, between 0 and 255
     * @param first the first channel, between 0 and 255
     * @param second the second channel, between 0 and 255
     * @param third the third channel, between 0 and 255
     * @return a new color, from the provided channels
     */
    @Contract(pure = true)
    public final int color(int alpha, int first, int second, int third) {
        return alpha << 24 | first << 16 | second << 8 | third;
    }

    /**
     * A color type that encodes hue and chroma-related values.
     */
    public static abstract class HueIntColorType extends IntColorType {
        /**
         * @return the chroma from 0 to 1 given a color
         */
        @Contract(pure = true)
        abstract protected float makeChroma(int color);

        @Contract(pure = true)
        public int hue(int color) {
            return first(color);
        }

        @Contract(pure = true)
        private float makeLimitedRed(float chroma, int hue) {
            float hPrime = hue / (255 / 6f);
            float hPrimeOffset = Math.abs(hue / (255 / 6f) - 3);
            float x = chroma * (1 - Math.abs(hPrime % 2 - 1));
            if (hPrimeOffset < 1) {
                return 0;
            } else if (hPrimeOffset < 2) {
                return x;
            }
            return chroma;
        }

        @Contract(pure = true)
        private float makeLimitedGreen(float chroma, int hue) {
            float hPrime = hue / (255 / 6f);
            float hPrimeOffset = Math.abs(hue / (255 / 6f) - 5) % 3;
            float x = chroma * (1 - Math.abs(hPrime % 2 - 1));
            if (hPrimeOffset < 1) {
                return 0;
            } else if (hPrimeOffset < 2) {
                return x;
            }
            return chroma;
        }

        @Contract(pure = true)
        private float makeLimitedBlue(float chroma, int hue) {
            float hPrime = hue / (255 / 6f);
            float hPrimeOffset = Math.abs(hue / (255 / 6f) - 1) % 3;
            float x = chroma * (1 - Math.abs(hPrime % 2 - 1));
            if (hPrimeOffset < 1) {
                return 0;
            } else if (hPrimeOffset < 2) {
                return x;
            }
            return chroma;
        }

        /**
         * @return what needs to be added to each 0 to 1 channel to provide the proper value, given a full color and chroma
         */
        @Contract(pure = true)
        abstract protected float makeMatch(int color, float chroma);

        @Override
        public int toARGB32(int color) {
            float chroma = makeChroma(color);
            int hue = hue(color);
            float m = makeMatch(color, chroma);
            float r = makeLimitedRed(chroma, hue) + m;
            float g = makeLimitedGreen(chroma, hue) + m;
            float b = makeLimitedBlue(chroma, hue) + m;
            return ColorTypes.ARGB32.color(alpha(color), Math.round(r * 255) & 0xFF, Math.round(g * 255) & 0xFF, Math.round(b * 255) & 0xFF);
        }

        @Override
        public int fromARGB32(int color) {
            int r = ColorTypes.ARGB32.red(color);
            int g = ColorTypes.ARGB32.green(color);
            int b = ColorTypes.ARGB32.blue(color);
            int xMax = Math.max(Math.max(r, g), b);
            int xMin = Math.min(Math.min(r, g), b);

            int chroma = xMax - xMin;
            int m = xMax==r ? 0 : xMax==g ? 1 : 2;
            int h = 0;
            float fR = r / 255f;
            float fG = g / 255f;
            float fB = b / 255f;
            float fChroma = chroma / 255f;

            if (chroma != 0) {
                var fH = switch (m) {
                    case 0 -> {
                        var value = ((fG - fB) / fChroma) % 6f;
                        if (value <= 0) yield value + 6f;
                        yield value;
                    }
                    case 1 -> (fB - fR) / fChroma + 2;
                    default -> (fR - fG) / fChroma + 4;
                };
                h = Mth.clamp(Math.round(fH / 6 * 0xFF), 0, 0xFF);
            }

            return makeColor(ColorTypes.ARGB32.alpha(color), h, chroma/255f, xMin/255f, xMax/255f);
        }

        /**
         * Finalized encoding of a color given some parameters.
         * @param alpha alpha value, from 0 to 255
         * @param hue hue, from 0 to 255
         * @param chroma chroma, from 0 to 1
         * @param xMin minimum value of RGB channels, from 0 to 1
         * @param xMax maximum value of RGB channels, from 0 to 1
         * @return an encoded color
         */
        @Contract(pure = true)
        abstract protected int makeColor(int alpha, int hue, float chroma, float xMin, float xMax);
    }

    /**
     * A color type whose channels can be linearly blended by alpha blending.
     */
    public abstract static class BlendingIntColorType extends IntColorType {
        /**
         * Calculates a composite color from two colors to layer
         * @param over the color to layer over the other
         * @param under the color to layer under the other
         * @return a composite color calculated by alpha blending
         */
        @Contract(pure = true)
        public int alphaBlend(int over, int under) {
            int aOver = alpha(over);
            int aUnder = alpha(under);
            int a = aOver + (aUnder * (255 - aOver) / 255);
            if (a == 0) return 0;
            int c1 = (first(over) * aOver + first(under) * aUnder * (255 - aOver) / 255) / a;
            int c2 = (second(over) * aOver + second(under) * aUnder * (255 - aOver) / 255) / a;
            int c3 = (third(over) * aOver + third(under) * aUnder * (255 - aOver) / 255) / a;
            return color(a, c1, c2, c3);
        }
    }
}
