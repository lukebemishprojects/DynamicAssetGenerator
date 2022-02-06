package dynamic_asset_generator.client.palette;

import dynamic_asset_generator.DynamicAssetGenerator;
import dynamic_asset_generator.client.util.IPalettePlan;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Stream;

public class Palette {
    private final ArrayList<ColorHolder> colors = new ArrayList<>();
    private final float inPaletteCutoff;

    public Palette(float inPaletteCutoff) {
        this.inPaletteCutoff = inPaletteCutoff;
    }

    public boolean isInPalette(ColorHolder color) {
        for (ColorHolder c : colors) {
            if ((Math.abs(c.getR()-color.getR()) < this.inPaletteCutoff) &&
                    (Math.abs(c.getG()-color.getG()) < this.inPaletteCutoff) &&
                    (Math.abs(c.getB()-color.getB()) < this.inPaletteCutoff)) {
                return true;
            }
        }
        return false;
    }

    public ColorHolder getColor(int i) {
        return colors.get(i);
    }

    public Stream<ColorHolder> getStream() {
        return colors.stream();
    }

    public void tryAdd(ColorHolder c) {
        if (!this.isInPalette(c)) {
            colors.add(c);
            Collections.sort(colors);
        }
    }

    public ColorHolder averageColor() {
        int t = 0;
        float r = 0;
        float g = 0;
        float b = 0;
        for (ColorHolder c : colors) {
            t++;
            r += c.getR();
            g += c.getG();
            b += c.getB();
        }
        return new ColorHolder(r/t,g/t,b/t);
    }

    public int closestTo(ColorHolder holder) {
        int index = 0;
        int outIndex = 0;
        double minDist = 2d;
        for (ColorHolder c : colors) {
            if (c.distanceTo(holder) < minDist) {
                outIndex = index;
                minDist = c.distanceTo(holder);
            }
            index++;
        }
        return outIndex;
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

    public int getSize() {
        return colors.size();
    }

    public static BufferedImage paletteCombinedImage(ResourceLocation outLoc, IPalettePlan plan) {
        boolean includeBackground = plan.includeBackground();
        int extend = plan.extend();
        try {
            BufferedImage b_img = plan.getBackground();
            BufferedImage o_img = plan.getOverlay();
            BufferedImage p_img = plan.getPaletted();
            //We don't care if they're square; we'll crop them. Currently, likely breaks animations...
            int o_dim = Math.min(o_img.getHeight(),o_img.getWidth());
            int b_dim = Math.min(b_img.getHeight(),b_img.getWidth());
            int p_dim = Math.min(p_img.getHeight(),p_img.getWidth());
            int w = Math.max(o_dim, Math.max(b_dim,p_dim));
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
            DynamicAssetGenerator.LOGGER.error("Error loading resources for image: {}",outLoc.toString());
            return null;
        }
    }
}
