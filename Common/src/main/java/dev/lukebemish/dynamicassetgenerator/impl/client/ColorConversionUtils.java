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

    public static ColorHolder xyz2rgb(ColorHolder xyz) {
        float r0 =  3.2406f * xyz.getX() - 1.5372f * xyz.getY() - 0.4986f * xyz.getZ();
        float g0 = -0.9689f * xyz.getX() + 1.8758f * xyz.getY() + 0.0415f * xyz.getZ();
        float b0 =  0.0557f * xyz.getX() - 0.2040f * xyz.getY() + 1.0570f * xyz.getZ();

        float r = (r0 > 0.0031308f) ? (float) (1.055f * Math.pow(r0, 1.f / 2.4f) - 0.055f) : (12.92f * r0);
        float g = (g0 > 0.0031308f) ? (float) (1.055f * Math.pow(g0, 1.f / 2.4f) - 0.055f) : (12.92f * g0);
        float b = (b0 > 0.0031308f) ? (float) (1.055f * Math.pow(b0, 1.f / 2.4f) - 0.055f) : (12.92f * b0);
        return new ColorHolder(r,g,b);
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

    public static ColorHolder lab2xyz(ColorHolder lab) {
        double _y = (lab.get_L()*_L_scale+16f)/116f;
        double _x = lab.get_a()*_a_scale/500f + _y;
        double _z = _y - lab.get_b()*_b_scale/200f;

        double x3 = _x*_x*_x;
        float x = (float) (((x3 > 0.008856f) ? x3 : ((_x - 16.f/116.f) / 7.787)) * refX);
        double y3 = _y*_y*_y;
        float y = (float) (((y3 > 0.008856f) ? y3 : ((_y - 16.f/116.f) / 7.787)) * refY);
        double z3 = _z*_z*_z;
        float z = (float) (((z3 > 0.008856f) ? z3 : ((_z - 16.f/116.f) / 7.787)) * refZ);

        return new ColorHolder(x,y,z);
    }

    public static ColorHolder rgb2lab(ColorHolder rgb) {
        return xyz2lab(rgb2xyz(rgb));
    }
    public static ColorHolder lab2rgb(ColorHolder lab) {
        return xyz2rgb(lab2xyz(lab));
    }
}
