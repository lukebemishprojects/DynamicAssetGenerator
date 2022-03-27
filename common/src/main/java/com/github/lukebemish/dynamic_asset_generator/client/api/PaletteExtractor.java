package com.github.lukebemish.dynamic_asset_generator.client.api;

import com.github.lukebemish.dynamic_asset_generator.client.palette.ColorHolder;
import com.github.lukebemish.dynamic_asset_generator.client.palette.Palette;
import com.github.lukebemish.dynamic_asset_generator.client.util.ImageUtils;
import com.github.lukebemish.dynamic_asset_generator.client.util.SupplierWithException;
import net.minecraft.resources.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PaletteExtractor {
    private static final List<PaletteExtractor> toRefresh = new ArrayList<>();

    public static void refresh() {
        for (PaletteExtractor i : toRefresh) {
            i.palettedImg = null;
            i.overlayImg = null;
        }
    }

    private final double closeCutoff;

    private final SupplierWithException<BufferedImage,IOException> background;
    private final SupplierWithException<BufferedImage,IOException> withOverlay;
    public final int extend;
    public final boolean trimTrailingPaletteLookup;
    private final boolean forceOverlayNeighbors;

    private BufferedImage overlayImg;
    private BufferedImage palettedImg;

    public PaletteExtractor(SupplierWithException<BufferedImage,IOException> background, SupplierWithException<BufferedImage,IOException> withOverlay, int extend, boolean trimTrailingPaletteLookup, boolean forceOverlayNeighbors, double closeCutoff) {
        this.background = background;
        this.withOverlay = withOverlay;
        this.extend = extend;
        this.trimTrailingPaletteLookup = trimTrailingPaletteLookup;
        this.forceOverlayNeighbors = forceOverlayNeighbors;
        this.closeCutoff = closeCutoff*1.5;
        toRefresh.add(this);
    }

    public PaletteExtractor(ResourceLocation background, ResourceLocation withOverlay, int extend, boolean trimTrailingPaletteLookup, boolean forceOverlayNeighbors, double closeCutoff) {
        this(()->ImageUtils.getImage(background),()->ImageUtils.getImage(withOverlay),extend,trimTrailingPaletteLookup,forceOverlayNeighbors,closeCutoff);
    }

    public PaletteExtractor(ResourceLocation background, ResourceLocation withOverlay, int extend) {
        this(background,withOverlay,extend,false,false,0.3);
    }

    public PaletteExtractor(ResourceLocation background, ResourceLocation withOverlay, int extend, boolean trimTrailingPaletteLookup, boolean forceOverlayNeighbors) {
        this(background,withOverlay,extend,trimTrailingPaletteLookup,forceOverlayNeighbors,0.3);
    }

    public BufferedImage getOverlayImg() throws IOException {
        if (overlayImg==null) {
            recalcImages();
        }
        return overlayImg;
    }

    public BufferedImage getPalettedImg() throws IOException {
        if (palettedImg==null) {
            recalcImages();
        }
        return palettedImg;
    }

    private void recalcImages() throws IOException {
        BufferedImage b_img = background.get();
        BufferedImage w_img = withOverlay.get();
        int b_dim = Math.min(b_img.getHeight(),b_img.getWidth());
        int w_dim = Math.min(w_img.getHeight(),w_img.getWidth());
        int dim = Math.max(b_dim,w_dim);
        int bs = dim/b_dim;
        int ws = dim/w_dim;
        //Assemble palette for b_img
        BufferedImage o_img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
        BufferedImage p_img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
        Palette backgroundPalette = Palette.extractPalette(b_img, extend);
        Palette withOverlayPalette = Palette.extractPalette(w_img, extend);
        int backgroundPaletteSize = backgroundPalette.getSize();

        double maxDiff = 0;
        for (ColorHolder c1 : withOverlayPalette.getStream().toList()) {
            for (ColorHolder c2 : withOverlayPalette.getStream().toList()) {
                double diff = c1.distanceToLS(c2);
                if (diff > maxDiff) maxDiff = diff;
            }
        }

        Palette frontColors = new Palette(5f/255f);
        ArrayList<PostCalcEvent> postQueue = new ArrayList<>();
        //write paletted image base stuff
        for (int x = 0; x < dim; x++) {
            for (int y = 0; y < dim; y++) {
                ColorHolder b_c = ColorHolder.fromColorInt(b_img.getRGB(x/bs,y/bs));
                ColorHolder w_c = ColorHolder.fromColorInt(w_img.getRGB(x/ws,y/ws));
                if (backgroundPalette.isInPalette(w_c)) {
                    int w_i = backgroundPalette.closestTo(w_c);
                    int b_i = backgroundPalette.closestTo(b_c);
                    if (w_i != b_i) {
                        p_img.setRGB(x,y,ColorHolder.toColorInt(new ColorHolder(1f/(backgroundPaletteSize-1)*w_i)));
                    }
                } else {
                    //the color sampled isn't in the palette. Now it gets painful...
                    //we could just try dumping it in the overlay, but that isn't going to work too well for some pixels.
                    //let's first find the minimum distance from the palette.
                    int distIndex = backgroundPalette.closestTo(w_c);
                    ColorHolder closestP = backgroundPalette.getColor(distIndex);
                    //Now let's check how close it is.
                    if (closestP.distanceToLS(w_c) <= closeCutoff * maxDiff) {
                        //Add it to the post-processing queue
                        p_img.setRGB(x,y,ColorHolder.toColorInt(new ColorHolder(1f/(backgroundPaletteSize-1)*distIndex)));
                        postQueue.add(new PostCalcEvent(x,y,w_c,closestP.distanceToLS(w_c)));
                    } else {
                        //It's too far away. Write to the overlay.
                        o_img.setRGB(x,y,ColorHolder.toColorInt(w_c));
                        frontColors.tryAdd(w_c);
                    }
                }
            }
        }
        for (PostCalcEvent e : postQueue) {
            int x = e.x();
            int y = e.y();
            ColorHolder wColor = e.wColor();
            int f_index = 0;
            int b_index = 0;
            double lowest = maxDiff*closeCutoff;
            float alpha = 0f;
            for (float a = 0.1f; a <= 0.25f; a+=0.05) {
                for (int f = 0; f < frontColors.getSize(); f++) {
                    for (int b = 0; b < backgroundPaletteSize; b++) {
                        ColorHolder bColor = backgroundPalette.getColor(b);
                        ColorHolder fColor = frontColors.getColor(f);
                        double dist = wColor.distanceToLS(ColorHolder.alphaBlend(fColor.withA(a), bColor));
                        if (dist < lowest) {
                            lowest = dist;
                            alpha = a;
                            f_index = f;
                            b_index = b;
                        }
                    }
                }
            }
            int frontIndex = frontColors.closestTo(wColor);
            int closeIndex = backgroundPalette.closestTo(wColor);
            if (wColor.distanceToLS(backgroundPalette.getColor(closeIndex)) > wColor.distanceToLS(frontColors.getColor(frontIndex))) {
                o_img.setRGB(x,y,ColorHolder.toColorInt(wColor.withA(1.0f)));
            } else if (wColor.distanceToLS(backgroundPalette.getColor(closeIndex)) < lowest) {
                p_img.setRGB(x,y,ColorHolder.toColorInt(backgroundPalette.getColor(closeIndex)));
            } else if (lowest < maxDiff*closeCutoff) {
                p_img.setRGB(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize-1) * b_index)));
                o_img.setRGB(x, y, ColorHolder.toColorInt(frontColors.getColor(f_index).withA(alpha)));
            } else {
                o_img.setRGB(x,y,ColorHolder.toColorInt(wColor.withA(1.0f)));
            }
        }

        if (trimTrailingPaletteLookup || forceOverlayNeighbors) {
            for (int x = 0; x < dim; x++) {
                for (int y = 0; y < dim; y++) {
                    boolean hasNeighbor = false;
                    boolean hasFullNeighbor = false;
                    int[] xs = {-1, -1, -1, 0, 0, 0, 1, 1, 1};
                    int[] ys = {-1, 0, 1, -1, 0, 1, -1, 0, 1};
                    for (int j = 0; j < xs.length; j++) {
                        int xt = x + xs[j];
                        int yt = y + ys[j];
                        if (0 <= xt && xt < dim && 0 <= yt && yt < dim) {
                            if (ColorHolder.fromColorInt(o_img.getRGB(xt, yt)).getA() != 0) {
                                hasNeighbor = true;
                            }
                            if (ColorHolder.fromColorInt(o_img.getRGB(xt, yt)).getA() == 1f) {
                                hasFullNeighbor = true;
                            }
                        }
                    }
                    if (trimTrailingPaletteLookup && !hasNeighbor) {
                        p_img.setRGB(x, y, 0);
                    }
                    if (forceOverlayNeighbors && hasFullNeighbor && ColorHolder.fromColorInt(o_img.getRGB(x,y)).getA() == 0) {
                        ColorHolder w_c = ColorHolder.fromColorInt(w_img.getRGB(x/ws,y/ws));
                        p_img.setRGB(x,y,ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize-1) * backgroundPalette.closestTo(w_c))));
                    }
                }
            }
        }

        this.overlayImg = o_img;
        this.palettedImg = p_img;
    }

    private static record PostCalcEvent(int x, int y, ColorHolder wColor,double dist) {}
}
