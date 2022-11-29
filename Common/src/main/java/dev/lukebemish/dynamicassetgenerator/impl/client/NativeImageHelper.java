/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client;

import com.mojang.blaze3d.platform.NativeImage;

public class NativeImageHelper {
    public static NativeImage of(NativeImage.Format format, int x, int y, boolean bl) {
        NativeImage ni = new NativeImage(format,x,y,bl);
        for (int i = 0; i<x; i++) {
            for (int j = 0; j<x; j++) {
                ni.setPixelRGBA(i,j,0);
            }
        }
        return ni;
    }
}
