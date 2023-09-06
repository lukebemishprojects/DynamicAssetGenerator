/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.colors;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.Contract;

/**
 * A type of color that has 4 16-bit channels, the first of which encodes the alpha value.
 */
public abstract class LongColorType {
    @Contract(pure = true)
    public final int alpha(long color) {
        return (int) (color >> 48) & 0xFFFF;
    }

    @Contract(pure = true)
    protected final int first(long color) {
        return (int) (color >> 32) & 0xFFFF;
    }

    @Contract(pure = true)
    protected final int second(long color) {
        return (int) (color >> 16) & 0xFFFF;
    }

    @Contract(pure = true)
    protected final int third(long color) {
        return (int) (color & 0xFFFF);
    }

    /**
     * @return the color, encoded as {@link ColorTypes.ARGB64}
     */
    @Contract(pure = true)
    public abstract long toARGB64(long color);

    /**
     * @return the provided {@link ColorTypes.ARGB64} color, encoded as this color type
     */
    @Contract(pure = true)
    public abstract long fromARGB64(long color);

    /**
     * Constructs a new color from channel values between 0x0000 and 0xFFFF.
     * @param alpha the alpha channel, between 0x0000 and 0xFFFF
     * @param first the first channel, between 0x0000 and 0xFFFF
     * @param second the second channel, between 0x0000 and 0xFFFF
     * @param third the third channel, between 0x0000 and 0xFFFF
     * @return a new color, from the provided channels
     */
    @Contract(pure = true)
    public long color(int alpha, int first, int second, int third) {
        return ((long) alpha) << 48 | ((long) first) << 32 | ((long) second) << 16 | ((long) third);
    }

    /**
     * A color type that encodes hue and chroma-related values.
     */
    public static abstract class HueLongColorType extends LongColorType {
        /**
         * @return the chroma from 0 to 1 given a color
         */
        @Contract(pure = true)
        abstract protected float makeChroma(long color);

        @Contract(pure = true)
        public int hue(long color) {
            return first(color);
        }

        @Contract(pure = true)
        private float makeLimitedRed(float chroma, int hue) {
            float hPrime = Math.abs(hue / (0xFFFF / 6f) - 3);
            float x = chroma * (1 - Math.abs(hPrime % 2 - 1));
            if (hPrime < 1) {
                return 0;
            } else if (hPrime < 2) {
                return x;
            }
            return chroma;
        }

        @Contract(pure = true)
        private float makeLimitedGreen(float chroma, int hue) {
            float hPrime = Math.abs(hue / (0xFFFF / 6f) - 5) % 3;
            float x = chroma * (1 - Math.abs(hPrime % 2 - 1));
            if (hPrime < 1) {
                return 0;
            } else if (hPrime < 2) {
                return x;
            }
            return chroma;
        }

        @Contract(pure = true)
        private float makeLimitedBlue(float chroma, int hue) {
            float hPrime = Math.abs(hue / (0xFFFF / 6f) - 1) % 3;
            float x = chroma * (1 - Math.abs(hPrime % 2 - 1));
            if (hPrime < 1) {
                return 0;
            } else if (hPrime < 2) {
                return x;
            }
            return chroma;
        }

        /**
         * @return what needs to be added to each 0 to 1 channel to provide the proper value, given a full color and chroma
         */
        @Contract(pure = true)
        abstract protected float makeMatch(long color, float chroma);

        @Override
        public long toARGB64(long color) {
            float chroma = makeChroma(color);
            int hue = hue(color);
            float m = makeMatch(color, chroma);
            float r = makeLimitedRed(chroma, hue) + m;
            float g = makeLimitedGreen(chroma, hue) + m;
            float b = makeLimitedBlue(chroma, hue) + m;
            return ColorTypes.ARGB64.color(alpha(color), Math.round(r * 0xFFFF) & 0xFFFF, Math.round(g * 0xFFFF) & 0xFFFF, Math.round(b * 0xFFFF) & 0xFFFF);
        }

        @Override
        public long fromARGB64(long color) {
            int r = ColorTypes.ARGB64.red(color);
            int g = ColorTypes.ARGB64.green(color);
            int b = ColorTypes.ARGB64.blue(color);
            int xMax = Math.max(Math.max(r, g), b);
            int xMin = Math.min(Math.min(r, g), b);

            int chroma = xMax - xMin;
            int m = xMax==r ? 0 : xMax==g ? 1 : 2;
            int h = 0;
            float fR = r / 0xFFFFp0f;
            float fG = g / 0xFFFFp0f;
            float fB = b / 0xFFFFp0f;
            float fChroma = chroma / 0xFFFFp0f;

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
                h = Mth.clamp(Math.round(fH / 6 * 0xFFFF), 0, 0xFFFF);
            }

            return makeColor(ColorTypes.ARGB64.alpha(color), h, fChroma, xMin/0xFFFFp0f, xMax/0xFFFFp0f);
        }

        /**
         * Finalized encoding of a color given some parameters.
         * @param alpha alpha value, from 0x0000 to 0xFFFF
         * @param hue hue, from 0x0000 to 0xFFFF
         * @param chroma chroma, from 0 to 1
         * @param xMin minimum value of RGB channels, from 0 to 1
         * @param xMax maximum value of RGB channels, from 0 to 1
         * @return an encoded color
         */
        @Contract(pure = true)
        abstract protected long makeColor(int alpha, int hue, float chroma, float xMin, float xMax);
    }

    /**
     * A color type whose channels can be linearly blended by alpha blending.
     */
    public abstract static class BlendingLongColorType extends LongColorType {
        /**
         * Calculates a composite color from two colors to layer
         * @param over the color to layer over the other
         * @param under the color to layer under the other
         * @return a composite color calculated by alpha blending
         */
        @Contract(pure = true)
        public long alphaBlend(long over, long under) {
            int aOver = alpha(over);
            int aUnder = alpha(under);
            int a = aOver + (aUnder * (0xFFFF - aOver) / 0xFFFF);
            if (a == 0) return 0;
            int c1 = (first(over) * aOver + first(under) * aUnder * (0xFFFF - aOver) / 0xFFFF) / a;
            int c2 = (second(over) * aOver + second(under) * aUnder * (0xFFFF - aOver) / 0xFFFF) / a;
            int c3 = (third(over) * aOver + third(under) * aUnder * (0xFFFF - aOver) / 0xFFFF) / a;
            return color(a, c1, c2, c3);
        }
    }
}
