package com.github.lukebemish.dynamic_asset_generator.client.api;

import com.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import com.github.lukebemish.dynamic_asset_generator.client.NativeImageHelper;
import com.github.lukebemish.dynamic_asset_generator.client.palette.ColorHolder;
import com.github.lukebemish.dynamic_asset_generator.client.palette.Palette;
import com.github.lukebemish.dynamic_asset_generator.client.util.Clusterer;
import com.github.lukebemish.dynamic_asset_generator.client.util.ImageUtils;
import com.github.lukebemish.dynamic_asset_generator.client.util.SupplierWithException;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PaletteExtractor {
    private static final List<PaletteExtractor> toRefresh = new ArrayList<>();

    public static void refresh() {
        for (PaletteExtractor i : toRefresh) {
            i.palettedImg.set(null);
            i.overlayImg.set(null);
        }
    }

    private boolean[] hasLogged = new boolean[2];
    private final double closeCutoff;

    private final SupplierWithException<NativeImage, IOException> background;
    private final SupplierWithException<NativeImage, IOException> withOverlay;
    public final int extend;
    public final boolean trimTrailingPaletteLookup;
    private final boolean forceOverlayNeighbors;

    private ThreadLocal<NativeImage> overlayImg = new ThreadLocal<>();
    private ThreadLocal<NativeImage> palettedImg = new ThreadLocal<>();

    private static final float N_CUTOFF_SCALE = 1.5f;

    public PaletteExtractor(SupplierWithException<NativeImage, IOException> background, SupplierWithException<NativeImage, IOException> withOverlay, int extend, boolean trimTrailingPaletteLookup, boolean forceOverlayNeighbors, double closeCutoff) {
        this.background = background;
        this.withOverlay = withOverlay;
        this.extend = extend;
        this.trimTrailingPaletteLookup = trimTrailingPaletteLookup;
        this.forceOverlayNeighbors = forceOverlayNeighbors;
        this.closeCutoff = closeCutoff;
        toRefresh.add(this);
    }

    public PaletteExtractor(ResourceLocation background, ResourceLocation withOverlay, int extend, boolean trimTrailingPaletteLookup, boolean forceOverlayNeighbors, double closeCutoff) {
        this(new ImageUtils.ImageGetter(background), new ImageUtils.ImageGetter(withOverlay), extend, trimTrailingPaletteLookup, forceOverlayNeighbors, closeCutoff);
    }

    public PaletteExtractor(ResourceLocation background, ResourceLocation withOverlay, int extend) {
        this(background, withOverlay, extend, false, false, 0.3);
    }

    public PaletteExtractor(ResourceLocation background, ResourceLocation withOverlay, int extend, boolean trimTrailingPaletteLookup, boolean forceOverlayNeighbors) {
        this(background, withOverlay, extend, trimTrailingPaletteLookup, forceOverlayNeighbors, 0.3);
    }

    public NativeImage getOverlayImg() throws IOException {
        if (overlayImg == null || overlayImg.get() == null) {
            recalcImages();
        }
        try {
            overlayImg.get().getPixelRGBA(0,0);
        } catch (IllegalStateException e) {
            overlayImg.get().close();
            if (palettedImg!=null&&palettedImg.get()!=null) palettedImg.get().close();
            recalcImages();
        }
        return overlayImg.get();
    }

    public NativeImage getPalettedImg() throws IOException {
        if (palettedImg == null || palettedImg.get() == null) {
            recalcImages();
        }
        try {
            palettedImg.get().getPixelRGBA(0,0);
        } catch (IllegalStateException e) {
            palettedImg.get().close();
            if (overlayImg!=null&&overlayImg.get()!=null) overlayImg.get().close();
            recalcImages();
        }
        return palettedImg.get();
    }

    private Holder recalcImagesAlternate() throws IOException {
        try (NativeImage b_img = background.get();
             NativeImage w_img = withOverlay.get()) {
            int b_dim = Math.min(b_img.getHeight(), b_img.getWidth());
            int w_dim = Math.min(w_img.getHeight(), w_img.getWidth());
            int dim = Math.max(b_dim, w_dim);
            int bs = dim / b_dim;
            int ws = dim / w_dim;
            //Assemble palette for b_img
            NativeImage o_img = NativeImageHelper.of(NativeImage.Format.RGBA, dim, dim, false);
            NativeImage p_img = NativeImageHelper.of(NativeImage.Format.RGBA, dim, dim, false);
            Palette backgroundPalette = Palette.extractPalette(b_img, extend);
            Palette withOverlayPalette = Palette.extractPalette(w_img, 0);
            int backgroundPaletteSize = backgroundPalette.getSize();

            Palette nbColors = new Palette();
            withOverlayPalette.getStream().forEach((c) -> {
                if (!backgroundPalette.isInPalette(c)) nbColors.tryAdd(c);
            });

            Clusterer clusterer = Clusterer.createFromPalettes(0.0d, Clusterer.Cluster::minDist, backgroundPalette, nbColors);

            Palette fColors = new Palette();

            int bgCat = clusterer.getCategory(backgroundPalette.getColor(0));

            if (nbColors.getSize() != 0) {
                nbColors.getStream().forEach((c) -> {
                    if (clusterer.getCategory(c) != bgCat) {
                        fColors.tryAdd(c);
                    }
                });
            }

            if (fColors.getSize() == 0 || nbColors.getSize() == 0) {
                for (int x = 0; x < dim; x++) {
                    for (int y = 0; y < dim; y++) {
                        o_img.setPixelRGBA(x, y, 0);
                        ColorHolder b_c = ColorHolder.fromColorInt(b_img.getPixelRGBA(x / bs, y / bs));
                        ColorHolder w_c = ColorHolder.fromColorInt(w_img.getPixelRGBA(x / ws, y / ws));
                        int w_i = backgroundPalette.closestTo(w_c);
                        int b_i = backgroundPalette.closestTo(b_c);
                        if (w_i != b_i)
                            p_img.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * w_i)));
                        else p_img.setPixelRGBA(x, y, 0);
                    }
                }
                String key = "";
                if (background instanceof ImageUtils.ImageGetter ig) key += " " + ig.rl.toString();
                if (withOverlay instanceof ImageUtils.ImageGetter ig) {
                    if (key.length() != 0) key += ",";
                    key += " " + ig.rl.toString();
                }
                if (!hasLogged[0]) DynamicAssetGenerator.LOGGER.warn("Supplied images{} for extraction contained no differing colors; only extracting palette shifts", key);
                hasLogged[0] = true;
                return new Holder(o_img, p_img, false);
            }

            //write paletted image base stuff
            for (int x = 0; x < dim; x++) {
                for (int y = 0; y < dim; y++) {
                    ColorHolder b_c = ColorHolder.fromColorInt(b_img.getPixelRGBA(x / bs, y / bs));
                    ColorHolder w_c = ColorHolder.fromColorInt(w_img.getPixelRGBA(x / ws, y / ws));
                    if (fColors.isInPalette(w_c)) {
                        o_img.setPixelRGBA(x, y, ColorHolder.toColorInt(w_c.withA(1)));
                    } else if (backgroundPalette.isInPalette(w_c)) {
                        int w_i = backgroundPalette.closestTo(w_c);
                        int b_i = backgroundPalette.closestTo(b_c);
                        if (w_i != b_i) {
                            p_img.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * w_i)));
                        }
                    } else {
                        int f_index = 0;
                        int b_index = 0;
                        double lowest = 200f;
                        float alpha = 0f;
                        boolean skipO = false;
                        for (float a = 0.1f; a <= 0.25f; a += 0.05) {
                            for (int b = 0; b < backgroundPaletteSize; b++) {
                                for (int f = 0; f < fColors.getSize(); f++) {
                                    ColorHolder bColor = backgroundPalette.getColor(b);
                                    ColorHolder fColor = fColors.getColor(f);
                                    double dist = w_c.distanceToLab(ColorHolder.alphaBlend(fColor.withA(a), bColor));
                                    if (dist < lowest) {
                                        lowest = dist;
                                        alpha = a;
                                        f_index = f;
                                        b_index = b;
                                        skipO = false;
                                    }
                                }
                                for (int b1 = 0; b1 < backgroundPalette.getSize(); b1++) {
                                    ColorHolder bColor = backgroundPalette.getColor(b);
                                    ColorHolder fColor = backgroundPalette.getColor(b1);
                                    ColorHolder blend = ColorHolder.alphaBlend(fColor.withA(a), bColor);
                                    double dist = w_c.distanceToLab(blend);
                                    if (dist < lowest) {
                                        lowest = dist;
                                        alpha = a;
                                        b_index = backgroundPalette.closestTo(blend);
                                        skipO = true;
                                    }
                                }
                            }
                        }
                        for (int f = 0; f < fColors.getSize(); f++) {
                            ColorHolder fColor = fColors.getColor(f);
                            double dist = w_c.distanceToLab(fColor);
                            if (dist < lowest) {
                                lowest = dist;
                                alpha = 1.0f;
                                f_index = f;
                                skipO = false;
                            }
                        }
                        p_img.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * b_index)));
                        if (!skipO) o_img.setPixelRGBA(x, y, fColors.getColor(f_index).withA(alpha).toInt());
                        if (alpha >= 1.0f) p_img.setPixelRGBA(x, y, new ColorHolder(0f).withA(0f).toInt());
                    }
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
                                if (ColorHolder.fromColorInt(o_img.getPixelRGBA(xt, yt)).getA() != 0) {
                                    hasNeighbor = true;
                                }
                                if (ColorHolder.fromColorInt(o_img.getPixelRGBA(xt, yt)).getA() == 1f) {
                                    hasFullNeighbor = true;
                                }
                            }
                        }
                        if (trimTrailingPaletteLookup && !hasNeighbor) {
                            p_img.setPixelRGBA(x, y, 0);
                        }
                        if (forceOverlayNeighbors && hasFullNeighbor && ColorHolder.fromColorInt(o_img.getPixelRGBA(x, y)).getA() == 0) {
                            ColorHolder w_c = ColorHolder.fromColorInt(w_img.getPixelRGBA(x / ws, y / ws));
                            p_img.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * backgroundPalette.closestTo(w_c))));
                        }
                    }
                }
            }
            return new Holder(o_img, p_img, nbColors.dist(nbColors) < nbColors.dist(backgroundPalette));
        }
    }

    private void recalcImages() throws IOException {
        try (NativeImage b_img = background.get();
             NativeImage w_img = withOverlay.get()) {
            int b_dim = Math.min(b_img.getHeight(), b_img.getWidth());
            int w_dim = Math.min(w_img.getHeight(), w_img.getWidth());
            int dim = Math.max(b_dim, w_dim);
            int bs = dim / b_dim;
            int ws = dim / w_dim;
            //Assemble palette for b_img
            NativeImage o_img = NativeImageHelper.of(NativeImage.Format.RGBA, dim, dim, false);
            NativeImage p_img = NativeImageHelper.of(NativeImage.Format.RGBA, dim, dim, false);
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

            Palette frontColors = new Palette(Palette.DEFAULT_CUTOFF);
            ArrayList<PostCalcEvent> postQueue = new ArrayList<>();
            //write paletted image base stuff
            for (int x = 0; x < dim; x++) {
                for (int y = 0; y < dim; y++) {
                    ColorHolder b_c = ColorHolder.fromColorInt(b_img.getPixelRGBA(x / bs, y / bs));
                    ColorHolder w_c = ColorHolder.fromColorInt(w_img.getPixelRGBA(x / ws, y / ws));
                    if (backgroundPalette.isInPalette(w_c)) {
                        int w_i = backgroundPalette.closestTo(w_c);
                        int b_i = backgroundPalette.closestTo(b_c);
                        if (w_i != b_i) {
                            p_img.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * w_i)));
                        }
                    } else {
                        //the color sampled isn't in the palette. Now it gets painful...
                        //we could just try dumping it in the overlay, but that isn't going to work too well for some pixels.
                        //let's first find the minimum distance from the palette.
                        int distIndex = backgroundPalette.closestTo(w_c);
                        ColorHolder closestP = backgroundPalette.getColor(distIndex);
                        //Now let's check how close it is.
                        if (closestP.distanceToLS(w_c) <= closeCutoff * maxDiff * N_CUTOFF_SCALE) {
                            //Add it to the post-processing queue
                            p_img.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * distIndex)));
                            postQueue.add(new PostCalcEvent(x, y, w_c, closestP.distanceToLS(w_c)));
                        } else {
                            //It's too far away. Write to the overlay.
                            o_img.setPixelRGBA(x, y, ColorHolder.toColorInt(w_c));
                            frontColors.tryAdd(w_c);
                        }
                    }
                }
            }

            Holder alt = recalcImagesAlternate();
            if (frontColors.getSize() == 0) {
                String key = "";
                if (background instanceof ImageUtils.ImageGetter ig) key += " " + ig.rl.toString();
                if (withOverlay instanceof ImageUtils.ImageGetter ig) {
                    if (key.length() != 0) key += ",";
                    key += " " + ig.rl.toString();
                }
                if (!hasLogged[1]) DynamicAssetGenerator.LOGGER.warn("Supplied images{} for extraction contained few differing colors; attempting clustering color extraction.", key);
                hasLogged[1] = true;
                this.overlayImg.set(alt.o());
                this.palettedImg.set(alt.p());
                o_img.close();
                p_img.close();
                return;
            } /*else if (alt.shouldUse()) {
                String key = "";
                if (background instanceof ImageUtils.ImageGetter ig) key += " "+ig.rl.toString();
                if (withOverlay instanceof ImageUtils.ImageGetter ig) {
                    if (key.length()!=0) key+=",";
                    key += " "+ig.rl.toString();
                }
                DynamicAssetGenerator.LOGGER.info("Supplied images{} for extraction used alternate extraction method.",key);
                this.overlayImg = alt.o();
                this.palettedImg = alt.p();
                return;
            } */ //Re-add once I figure out how the heck shouldUse should be calculated.

            for (PostCalcEvent e : postQueue) {
                int x = e.x();
                int y = e.y();
                ColorHolder wColor = e.wColor();
                int f_index = 0;
                int b_index = 0;
                double lowest = 200f;
                float alpha = 0f;
                boolean skipO = false;
                for (float a = 0.1f; a <= 0.25f; a += 0.05) {
                    for (int b = 0; b < backgroundPaletteSize; b++) {
                        for (int f = 0; f < frontColors.getSize(); f++) {
                            ColorHolder bColor = backgroundPalette.getColor(b);
                            ColorHolder fColor = frontColors.getColor(f);
                            double dist = wColor.distanceToLab(ColorHolder.alphaBlend(fColor.withA(a), bColor));
                            if (dist < lowest) {
                                lowest = dist;
                                alpha = a;
                                f_index = f;
                                b_index = b;
                                skipO = false;
                            }
                        }
                        for (int b1 = 0; b1 < backgroundPalette.getSize(); b1++) {
                            ColorHolder bColor = backgroundPalette.getColor(b);
                            ColorHolder fColor = backgroundPalette.getColor(b1);
                            ColorHolder blend = ColorHolder.alphaBlend(fColor.withA(a), bColor);
                            double dist = wColor.distanceToLab(blend);
                            if (dist < lowest) {
                                lowest = dist;
                                alpha = a;
                                b_index = backgroundPalette.closestTo(blend);
                                skipO = true;
                            }
                        }
                    }
                }
                for (int f = 0; f < frontColors.getSize(); f++) {
                    ColorHolder fColor = frontColors.getColor(f);
                    double dist = wColor.distanceToLab(fColor);
                    if (dist < lowest) {
                        lowest = dist;
                        alpha = 1.0f;
                        f_index = f;
                        skipO = false;
                    }
                }
                p_img.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * b_index)));
                if (!skipO)
                    o_img.setPixelRGBA(x, y, ColorHolder.toColorInt(frontColors.getColor(f_index).withA(alpha)));
                int frontIndex = frontColors.closestTo(wColor);
                int closeIndex = backgroundPalette.closestTo(wColor);
                if (wColor.distanceToLS(backgroundPalette.getColor(closeIndex)) > wColor.distanceToLS(frontColors.getColor(frontIndex))) {
                    o_img.setPixelRGBA(x, y, ColorHolder.toColorInt(wColor.withA(1.0f)));
                    alpha = 1.0f;
                }
                if (alpha >= 1.0f) p_img.setPixelRGBA(x, y, new ColorHolder(0f).withA(0f).toInt());
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
                                if (ColorHolder.fromColorInt(o_img.getPixelRGBA(xt, yt)).getA() != 0) {
                                    hasNeighbor = true;
                                }
                                if (ColorHolder.fromColorInt(o_img.getPixelRGBA(xt, yt)).getA() == 1f) {
                                    hasFullNeighbor = true;
                                }
                            }
                        }
                        if (trimTrailingPaletteLookup && !hasNeighbor) {
                            p_img.setPixelRGBA(x, y, 0);
                        }
                        if (forceOverlayNeighbors && hasFullNeighbor && ColorHolder.fromColorInt(o_img.getPixelRGBA(x, y)).getA() == 0) {
                            ColorHolder w_c = ColorHolder.fromColorInt(w_img.getPixelRGBA(x / ws, y / ws));
                            p_img.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * backgroundPalette.closestTo(w_c))));
                        }
                    }
                }
            }

            this.overlayImg.set(o_img);
            this.palettedImg.set(p_img);
        }
    }

    private record PostCalcEvent(int x, int y, ColorHolder wColor, double dist) {}

    private record Holder(NativeImage o, NativeImage p, boolean shouldUse) {}
}
