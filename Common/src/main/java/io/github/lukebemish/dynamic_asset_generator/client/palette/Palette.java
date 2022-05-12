package io.github.lukebemish.dynamic_asset_generator.client.palette;

import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.client.NativeImageHelper;
import io.github.lukebemish.dynamic_asset_generator.client.util.IPalettePlan;
import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class Palette {
    private final ArrayList<ColorHolder> colors;
    private final float inPaletteCutoff;

    public Palette(float inPaletteCutoff) {
        this.colors = new ArrayList<>();
        this.inPaletteCutoff = inPaletteCutoff;
    }
    public Palette(List<ColorHolder> colors) {
        this.colors = new ArrayList<>(colors);
        this.colors.sort(ColorHolder::compareTo);
        this.inPaletteCutoff = DEFAULT_CUTOFF;
    }
    public Palette() {
        this(DEFAULT_CUTOFF);
    }

    public ColorHolder getCentroid() {
        AtomicReference<Float> x= new AtomicReference<>((float) 0);
        AtomicReference<Float> y= new AtomicReference<>((float) 0);
        AtomicReference<Float> z= new AtomicReference<>((float) 0);
        AtomicInteger count= new AtomicInteger();
        getStream().forEach((c)->{
            x.updateAndGet(v -> (float) (v + c.getR()));
            y.updateAndGet(v -> (float) (v + c.getG()));
            z.updateAndGet(v -> (float) (v + c.getB()));
            count.getAndIncrement();
        });
        int c = count.get();
        return new ColorHolder(x.get()/c,y.get()/c,z.get()/c);
    }

    public double getStdDev() {
        AtomicReference<Double> sum = new AtomicReference<>((double) 0);
        ColorHolder c = getCentroid();
        getStream().forEach(x->{
            double d = c.distanceToLab(x);
            sum.updateAndGet(v -> v + d * d);
        });
        return Math.sqrt(sum.get());
    }

    public boolean isInPalette(ColorHolder color) {
        return this.isInPalette(color, this.inPaletteCutoff);
    }

    public boolean isInPalette(ColorHolder color, float cutoff) {
        if (color.getA() == 0) return false;
        for (ColorHolder c : colors) {
            //check if every channel is less than the cutoff apart.
            if (Math.abs(c.getR()-color.getR())<cutoff && Math.abs(c.getG()-color.getG())<cutoff && Math.abs(c.getB()-color.getB())<cutoff) {
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
            if (c.distanceToLab(holder) < minDist) {
                outIndex = index;
                minDist = c.distanceToLab(holder);
            }
            index++;
        }
        return outIndex;
    }

    public static Palette extractPalette(NativeImage image, int extend, float inPaletteCutoff) {
        int w = image.getWidth();
        int h = image.getHeight();
        Palette palette = new Palette(inPaletteCutoff);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int c_int = image.getPixelRGBA(x,y);
                ColorHolder c = ColorHolder.fromColorInt(c_int);
                if (c.getA()!=0 && !palette.isInPalette(c)) {
                    palette.colors.add(new ColorHolder(c.getR(), c.getG(), c.getB()));
                }
            }
        }
        palette.extendPalette(extend);
        return palette;
    }

    public static final float DEFAULT_CUTOFF = 2f/255f;

    public static Palette extractPalette(NativeImage image, int extend) {
        return extractPalette(image,extend,DEFAULT_CUTOFF);
    }

    public Palette extendPalette(int extend) {
        Collections.sort(this.colors);
        // Extend the palette if necessary (if it's less than 6 colors)
        while (this.colors.size() < extend) {
            ColorHolder high = this.colors.get(this.colors.size()-1);
            ColorHolder low = this.colors.get(0);
            ColorHolder highNew = new ColorHolder(high.getR() * .9f + .1f, high.getG() * .9f + .1f, high.getB() * .9f + .1f);
            ColorHolder lowNew = new ColorHolder(low.getR() * .9f, low.getG() * .9f, low.getB() * .9f);
            this.colors.add(highNew);
            this.colors.add(0,lowNew);
        }
        return this;
    }

    public ColorHolder getColorAtRamp(float pos) {
        int index = Math.round(pos*(colors.size()-1));
        return colors.get(index);
    }

    public int getSize() {
        return colors.size();
    }

    public static NativeImage paletteCombinedImage(IPalettePlan plan) {
        boolean includeBackground = plan.includeBackground();
        int extend = plan.extend();
        try (NativeImage b_img = plan.getBackground();
             NativeImage o_img = plan.getOverlay();
             NativeImage p_img = plan.getPaletted()) {
            //We don't care if they're square; we'll crop them. Currently, likely breaks animations...
            int o_dim = Math.min(o_img.getHeight(),o_img.getWidth());
            int b_dim = Math.min(b_img.getHeight(),b_img.getWidth());
            int p_dim = Math.min(p_img.getHeight(),p_img.getWidth());
            int w = Math.max(o_dim, Math.max(b_dim,p_dim));
            int os = w/o_img.getWidth();
            int bs = w/b_img.getWidth();
            int ps = w/p_img.getWidth();
            NativeImage out_img = NativeImageHelper.of(NativeImage.Format.RGBA,w,w,false);
            Palette backgroundPalette = extractPalette(b_img, extend,1f/255f);
            ColorHolder maxPaletteKey = new ColorHolder(0,0,0);
            ColorHolder minPaletteKey = new ColorHolder(1,1,1);
            if (plan.stretchPaletted()) {
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < w; y++) {
                        ColorHolder colorThis = ColorHolder.fromColorInt(p_img.getPixelRGBA(x / ps, y / ps)).withA(1.0f);
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
                        outVal = ColorHolder.fromColorInt(b_img.getPixelRGBA(x / bs, y / bs));
                    } else {
                        outVal = new ColorHolder(0f,0f,0f,0f);
                    }
                    // Add color by palette
                    ColorHolder p_val = ColorHolder.fromColorInt(p_img.getPixelRGBA(x/ps,y/ps));
                    if (p_val.getA() > 0f) {
                        float ramp = (p_val.getR() + p_val.getG() + p_val.getB()) / 3f;
                        if (maxAvg > 0 && minAvg < 1 && range > 0) {
                            ramp = (ramp-minAvg)/range;
                        }
                        ColorHolder palettedColor = backgroundPalette.getColorAtRamp(ramp);
                        palettedColor = palettedColor.withA(p_val.getA());
                        outVal = ColorHolder.alphaBlend(palettedColor, outVal);
                    }
                    ColorHolder overlayC = ColorHolder.fromColorInt(o_img.getPixelRGBA(x/os,y/os));
                    outVal = ColorHolder.alphaBlend(overlayC, outVal);
                    out_img.setPixelRGBA(x,y,ColorHolder.toColorInt(outVal));
                }
            }
            return out_img;
        } catch (IOException e) {
            DynamicAssetGenerator.LOGGER.error("Error loading resources for image", e);
            return null;
        }
    }

    public double dist(Palette other) {
        double d = 0;
        int c = 0;
        ColorHolder cent = other.getCentroid();
        for (ColorHolder c1 : this.colors) {
            c++;
            d+=c1.distanceToLS(cent);
        }
        return d/c;
    }

    public float getCutoff() {
        return this.inPaletteCutoff;
    }
}
