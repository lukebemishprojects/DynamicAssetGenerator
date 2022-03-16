package com.github.lukebemish.dynamic_asset_generator.client.util;

import com.github.lukebemish.dynamic_asset_generator.client.palette.ColorHolder;

import java.awt.image.BufferedImage;

public class SafeImageExtraction {
    public static int get(BufferedImage image, int x, int y) {
        if (x<0 || x>=image.getWidth() || y<0 || y>=image.getHeight()) {
            return 0;
        }
        return  image.getRGB(x,y);
    }
    public static ColorHolder getColor(BufferedImage image, int x, int y) {
        return ColorHolder.fromColorInt(get(image,x,y));
    }
}
