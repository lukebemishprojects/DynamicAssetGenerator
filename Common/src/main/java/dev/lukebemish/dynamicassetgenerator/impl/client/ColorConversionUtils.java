/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client;

import dev.lukebemish.dynamicassetgenerator.impl.client.palette.ColorHolder;

public class ColorConversionUtils {
    public static ColorHolder rgb2xyz(ColorHolder rgb) {
        double r = ((rgb.getR() > 0.04045f) ? Math.pow((rgb.getR() + 0.055f) / 1.055f, 2.4f) : (rgb.getR() / 12.92f));
        double g = ((rgb.getG() > 0.04045f) ? Math.pow((rgb.getG() + 0.055f) / 1.055f, 2.4f) : (rgb.getG() / 12.92f));
        double b = ((rgb.getB() > 0.04045f) ? Math.pow((rgb.getB() + 0.055f) / 1.055f, 2.4f) : (rgb.getB() / 12.92f));

        float x = (float) (0.4124f * r + 0.3576f * g + 0.1805f * b);
        float y = (float) (0.2126f * r + 0.7152f * g + 0.0722f * b);
        float z = (float) (0.0193f * r + 0.1192f * g + 0.9505f * b);
        return new ColorHolder(x,y,z);
    }

    // D65/2
    private static final double refX = 95.047/100;
    private static final double refY = 100.0/100;
    private static final double refZ = 108.883/100;

    public static ColorHolder xyz2lab(ColorHolder xyz) {
        double _x = xyz.getX()/refX;
        double _y = xyz.getY()/refY;
        double _z = xyz.getZ()/refZ;

        _x = (_x > 0.008856f) ? Math.cbrt(_x) : ((7.787f * _x) + 16.f/116.f);
        _y = (_y > 0.008856f) ? Math.cbrt(_y) : ((7.787f * _y) + 16.f/116.f);
        _z = (_z > 0.008856f) ? Math.cbrt(_z) : ((7.787f * _z) + 16.f/116.f);

        double _L = (116 * _y) - 16;
        double _a = 500 * (_x - _y);
        double _b = 200 * (_y - _z);
        return new ColorHolder((float)_L/_L_scale, (float)_a/_a_scale,(float)_b/_b_scale);
    }

    private static final float _L_scale = 100;
    private static final float _a_scale = 255;
    private static final float _b_scale = 255;

    public static ColorHolder rgb2lab(ColorHolder rgb) {
        return xyz2lab(rgb2xyz(rgb));
    }
}
