package com.github.lukebemish.dynamic_asset_generator.client.api;

import com.github.lukebemish.dynamic_asset_generator.client.util.IPalettePlan;
import com.github.lukebemish.dynamic_asset_generator.client.util.ImageUtils;
import net.minecraft.resources.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class ForegroundTransferType implements IPalettePlan {
    private final ResourceLocation background;
    private final PaletteExtractor extractor;
    private final boolean includeBackground;
    private final boolean stretchPaletted;

    public ForegroundTransferType(PaletteExtractor extractor, ResourceLocation background, boolean includeBackground, boolean stretchPaletted) {
        this.background = background;
        this.extractor = extractor;
        this.includeBackground = includeBackground;
        this.stretchPaletted = stretchPaletted;
    }

    @Override
    public BufferedImage getBackground() throws IOException {
        return ImageUtils.getImage(background);
    }

    @Override
    public BufferedImage getOverlay() throws IOException {
        return extractor.getOverlayImg();
    }

    @Override
    public BufferedImage getPaletted() throws IOException {
        return extractor.getPalettedImg();
    }

    @Override
    public boolean includeBackground() {
        return this.includeBackground;
    }

    @Override
    public boolean stretchPaletted() {
        return this.stretchPaletted;
    }

    @Override
    public int extend() {
        return extractor.extend;
    }
}
