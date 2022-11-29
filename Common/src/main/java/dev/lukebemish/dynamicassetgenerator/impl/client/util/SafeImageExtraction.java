/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client.util;

import dev.lukebemish.dynamicassetgenerator.impl.client.palette.ColorHolder;
import com.mojang.blaze3d.platform.NativeImage;

public class SafeImageExtraction {
    private SafeImageExtraction() {}
    public static int get(NativeImage image, int x, int y) {
        if (x<0 || x>=image.getWidth() || y<0 || y>=image.getHeight()) {
            return 0;
        }
        return  image.getPixelRGBA(x,y);
    }
    public static ColorHolder getColor(NativeImage image, int x, int y) {
        return ColorHolder.fromColorInt(get(image,x,y));
    }
}
