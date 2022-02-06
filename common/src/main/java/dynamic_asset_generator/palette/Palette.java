package dynamic_asset_generator.palette;

import dynamic_asset_generator.api.PlannedPaletteCombinedImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import dynamic_asset_generator.util.ImageUtils;
import dynamic_asset_generator.DynamicAssetGenerator;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

@Environment(EnvType.CLIENT)
public class Palette {
    private final ArrayList<ColorHolder> colors = new ArrayList<>();
    private final float inPaletteCutoff;

    public Palette(float inPaletteCutoff) {
        this.inPaletteCutoff = inPaletteCutoff;
    }

    private boolean isInPalette(ColorHolder color) {
        for (ColorHolder c : colors) {
            if ((Math.abs(c.getR()-color.getR()) < this.inPaletteCutoff) &&
                    (Math.abs(c.getG()-color.getG()) < this.inPaletteCutoff) &&
                    (Math.abs(c.getB()-color.getB()) < this.inPaletteCutoff)) {
                return true;
            }
        }
        return false;
    }

    public static Palette extractPalette(BufferedImage image, int extend) {
        int w = image.getWidth();
        int h = image.getHeight();
        Palette palette = new Palette(5f/255f);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int c_int = image.getRGB(x,y);
                ColorHolder c = ColorHolder.fromColorInt(c_int);
                if (!palette.isInPalette(c)) {
                    palette.colors.add(new ColorHolder(c.getR(), c.getG(), c.getB()));
                }
            }
        }
        Collections.sort(palette.colors);
        // Extend the palette if necessary (if it's less than 6 colors)
        while (palette.colors.size() < extend) {
            ColorHolder high = palette.colors.get(palette.colors.size()-1);
            ColorHolder low = palette.colors.get(0);
            ColorHolder highNew = new ColorHolder(high.getR() * .9f + .1f, high.getG() * .9f + .1f, high.getB() * .9f + .1f);
            ColorHolder lowNew = new ColorHolder(low.getR() * .9f, low.getG() * .9f, low.getB() * .9f);
            palette.colors.add(highNew);
            palette.colors.add(0,lowNew);
        }
        return palette;
    }

    public ColorHolder getColorAtRamp(float pos) {
        int index = Math.round(pos*(colors.size()-1));
        return colors.get(index);
    }

    public static BufferedImage paletteCombinedImage(PlannedPaletteCombinedImage plan) {
        ResourceLocation background = plan.background();
        ResourceLocation overlay = plan.overlay();
        ResourceLocation paletted = plan.paletted();
        boolean includeBackground = plan.includeBackground();
        int extend = plan.extend();
        try {
            BufferedImage b_img = ImageUtils.getImage(background);
            BufferedImage o_img = ImageUtils.getImage(overlay);
            BufferedImage p_img = ImageUtils.getImage(paletted);
            if (!((b_img.getHeight() == b_img.getWidth()) && (o_img.getHeight() == o_img.getWidth()) && (p_img.getHeight() == p_img.getWidth()))) {
                DynamicAssetGenerator.LOGGER.error("Images at locations are not square: {}, {}, {}",background.toString(),overlay.toString(),paletted.toString());
                return null;
            }
            int w = Math.max(b_img.getWidth(), Math.max(o_img.getWidth(), p_img.getWidth()));
            int os = w/o_img.getWidth();
            int bs = w/b_img.getWidth();
            int ps = w/p_img.getWidth();
            BufferedImage out_img = new BufferedImage(w,w,BufferedImage.TYPE_INT_ARGB);
            Palette backgroundPalette = extractPalette(b_img, extend);
            ColorHolder maxPaletteKey = new ColorHolder(0,0,0);
            ColorHolder minPaletteKey = new ColorHolder(1,1,1);
            if (plan.stretchPaletted()) {
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < w; y++) {
                        ColorHolder colorThis = ColorHolder.fromColorInt(p_img.getRGB(x / ps, y / ps)).withA(1.0f);
                        if (colorThis.compareTo(maxPaletteKey) > 0) {
                            maxPaletteKey = colorThis;
                        } else if (colorThis.compareTo(minPaletteKey) < 0) {
                            minPaletteKey = colorThis;
                        }
                    }
                }
            }
            float maxAvg = (maxPaletteKey.getR()+maxPaletteKey.getG()+maxPaletteKey.getB())/3f;
            float minAvg = (minPaletteKey.getR()+minPaletteKey.getG()+minPaletteKey.getB())/3f;
            float range = maxAvg - minAvg;
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < w; y++) {
                    ColorHolder outVal;
                    if (includeBackground) {
                        outVal = ColorHolder.fromColorInt(b_img.getRGB(x / bs, y / bs));
                    } else {
                        outVal = new ColorHolder(0f,0f,0f,0f);
                    }
                    // Add color by palette
                    ColorHolder p_val = ColorHolder.fromColorInt(p_img.getRGB(x/ps,y/ps));
                    if (p_val.getA() > 0f) {
                        float ramp = (p_val.getR() + p_val.getG() + p_val.getB()) / 3f;
                        if (maxAvg > 0 && minAvg < 1 && range > 0) {
                            ramp = (ramp-minAvg)/range;
                        }
                        ColorHolder palettedColor = backgroundPalette.getColorAtRamp(ramp);
                        palettedColor = palettedColor.withA(p_val.getA());
                        outVal = ColorHolder.alphaBlend(palettedColor, outVal);
                    }
                    ColorHolder overlayC = ColorHolder.fromColorInt(o_img.getRGB(x/os,y/os));
                    outVal = ColorHolder.alphaBlend(overlayC, outVal);
                    out_img.setRGB(x,y,ColorHolder.toColorInt(outVal));
                }
            }
            return out_img;
        } catch (IOException e) {
            DynamicAssetGenerator.LOGGER.error("Error loading resources: {}, {}, {}",background.toString(),overlay.toString(),paletted.toString());
            return null;
        }
    }
}
