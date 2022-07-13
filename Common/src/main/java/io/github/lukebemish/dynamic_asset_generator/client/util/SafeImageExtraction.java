package io.github.lukebemish.dynamic_asset_generator.client.util;

import io.github.lukebemish.dynamic_asset_generator.client.palette.ColorHolder;
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
