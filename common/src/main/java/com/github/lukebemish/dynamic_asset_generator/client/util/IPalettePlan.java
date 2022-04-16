package com.github.lukebemish.dynamic_asset_generator.client.util;

import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;

public interface IPalettePlan {
    NativeImage getBackground() throws IOException;
    NativeImage getOverlay() throws IOException;
    NativeImage getPaletted() throws IOException;
    boolean includeBackground();
    boolean stretchPaletted();
    int extend();
}
