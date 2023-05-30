/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client.palette;

import dev.lukebemish.dynamicassetgenerator.impl.client.ColorConversionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ColorHolder implements Comparable<ColorHolder> {
    private final float r;
    private final float g;
    private final float b;
    private final float a;

    public static ColorHolder fromColorInt(int color) {
        return new ColorHolder(
                (color    &0xFF)/255f,
                (color>> 8&0xFF)/255f,
                (color>>16&0xFF)/255f,
                (color>>24&0xFF)/255f
        );
    }

    public ColorHolder(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = 1.0f;
    }

    public ColorHolder(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public float getR() {
        return r;
    }

    public float getG() {
        return g;
    }

    public float getB() {
        return b;
    }

    public float getA() {
        return a;
    }

    @Override
    public int compareTo(@NotNull ColorHolder o) {
        ColorHolder c1 = this.toCIELAB();
        ColorHolder c2 = o.toCIELAB();
        float mySum = c1.r;
        float otherSum = c2.r;
        if (mySum > otherSum) {
            return 1;
        } else if (mySum < otherSum) {
            return -1;
        }
        return 0;
    }

    public ColorHolder toHLS() {
        float max = max(r,g,b);
        float min = min(r,g,b);
        float h,s,l;
        l = (max+min)/2;
        if (max==min) {
            h=s=0;
        } else {
            float d = max-min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            int m = max==r ? 0 : max==g ? 1 : 2;
            h = switch (m) {
                case 0 -> (g - b) / d + (g < b ? 6 : 0);
                case 1 -> (b - r) / d + 2;
                default -> (r - g) / d + 4;
            };
            h/=6;
        }
        return new ColorHolder(h,l,s);
    }

    public ColorHolder toCIELAB() {
        return ColorConversionUtils.rgb2lab(this);
    }

    private static float max(float a, float b, float c) {
        return Math.max(Math.max(a,b),c);
    }

    private static float min(float a, float b, float c) {
        return Math.min(Math.min(a,b),c);
    }

    public float getH() {
        return this.r;
    }

    public float getL() {
        return this.g;
    }

    public float getS() {
        return this.b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColorHolder that = (ColorHolder) o;
        return that.r == r && that.g == g && that.b == b && that.a == a;
    }

    @Override
    public int hashCode() {
        return Objects.hash(r, g, b, a);
    }

    public float getX() {
        return r;
    }
    public float getY() {
        return g;
    }
    public float getZ() {
        return b;
    }

}
