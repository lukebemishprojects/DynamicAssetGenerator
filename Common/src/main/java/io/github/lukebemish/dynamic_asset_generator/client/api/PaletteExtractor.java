package io.github.lukebemish.dynamic_asset_generator.client.api;

import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.Pair;
import io.github.lukebemish.dynamic_asset_generator.client.NativeImageHelper;
import io.github.lukebemish.dynamic_asset_generator.client.palette.ColorHolder;
import io.github.lukebemish.dynamic_asset_generator.client.palette.Palette;
import io.github.lukebemish.dynamic_asset_generator.client.util.Clusterer;
import io.github.lukebemish.dynamic_asset_generator.client.util.ImageUtils;
import io.github.lukebemish.dynamic_asset_generator.client.util.SupplierWithException;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaletteExtractor {
    private static final List<PaletteExtractor> toRefresh = new ArrayList<>();
    private static final int[] X_SAMPLING_ORDER = new int[]{-1, -1, -1, 0, 0, 0, 1, 1, 1};
    private static final int[] Y_SAMPLING_ORDER = new int[]{-1, 0, 1, -1, 0, 1, -1, 0, 1};
    private boolean fillHoles =false;

    public static void refresh() {
        for (PaletteExtractor i : toRefresh) {
            i.palettedImg.set(null);
            i.overlayImg.set(null);
        }
    }

    private final boolean[] hasLogged = new boolean[2];
    private final double closeCutoff;

    private final SupplierWithException<NativeImage, IOException> background;
    private final SupplierWithException<NativeImage, IOException> withOverlay;
    public final int extend;
    public final boolean trimTrailingPaletteLookup;
    private final boolean forceOverlayNeighbors;

    private final ThreadLocal<NativeImage> overlayImg = new ThreadLocal<>();
    private final ThreadLocal<NativeImage> palettedImg = new ThreadLocal<>();

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
        if (overlayImg.get() == null) {
            recalcImages();
        }
        try {
            overlayImg.get().getPixelRGBA(0,0);
        } catch (IllegalStateException e) {
            overlayImg.get().close();
            if (palettedImg.get() != null) palettedImg.get().close();
            recalcImages();
        }
        return overlayImg.get();
    }

    public NativeImage getPalettedImg() throws IOException {
        if (palettedImg.get() == null) {
            recalcImages();
        }
        try {
            palettedImg.get().getPixelRGBA(0,0);
        } catch (IllegalStateException e) {
            palettedImg.get().close();
            if (overlayImg.get() != null) overlayImg.get().close();
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

            trimAndOverlay(dim, ws, o_img, p_img, w_img, backgroundPaletteSize, backgroundPalette);
            return new Holder(o_img, p_img, nbColors.dist(nbColors) < nbColors.dist(backgroundPalette));
        }
    }

    private void trimAndOverlay(int dim, int ws, NativeImage oImg, NativeImage pImg, NativeImage wImg, int backgroundPaletteSize, Palette backgroundPalette) {
        if (trimTrailingPaletteLookup || forceOverlayNeighbors) {
            for (int x = 0; x < dim; x++) {
                for (int y = 0; y < dim; y++) {
                    boolean hasNeighbor = false;
                    boolean hasFullNeighbor = false;
                    for (int j = 0; j < X_SAMPLING_ORDER.length; j++) {
                        int xt = x + X_SAMPLING_ORDER[j];
                        int yt = y + Y_SAMPLING_ORDER[j];
                        if (0 <= xt && xt < dim && 0 <= yt && yt < dim) {
                            if (ColorHolder.fromColorInt(oImg.getPixelRGBA(xt, yt)).getA() != 0) {
                                hasNeighbor = true;
                            }
                            if (ColorHolder.fromColorInt(oImg.getPixelRGBA(xt, yt)).getA() == 1f) {
                                hasFullNeighbor = true;
                            }
                        }
                    }
                    if (trimTrailingPaletteLookup && !hasNeighbor) {
                        pImg.setPixelRGBA(x, y, 0);
                    }
                    if (forceOverlayNeighbors && hasFullNeighbor && ColorHolder.fromColorInt(oImg.getPixelRGBA(x, y)).getA() == 0) {
                        ColorHolder wC = ColorHolder.fromColorInt(wImg.getPixelRGBA(x / ws, y / ws));
                        pImg.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * backgroundPalette.closestTo(wC))));
                    }
                }
            }
        }
    }

    private void recalcImages() throws IOException {
        try (NativeImage b_img = background.get();
             NativeImage wImg = withOverlay.get()) {
            int b_dim = Math.min(b_img.getHeight(), b_img.getWidth());
            int w_dim = Math.min(wImg.getHeight(), wImg.getWidth());
            int dim = Math.max(b_dim, w_dim);
            int bs = dim / b_dim;
            int ws = dim / w_dim;
            //Assemble palette for b_img
            NativeImage oImg = NativeImageHelper.of(NativeImage.Format.RGBA, dim, dim, false);
            NativeImage pImg = NativeImageHelper.of(NativeImage.Format.RGBA, dim, dim, false);
            Palette backgroundPalette = Palette.extractPalette(b_img, extend);
            Palette withOverlayPalette = Palette.extractPalette(wImg, extend);
            int backgroundPaletteSize = backgroundPalette.getSize();

            double maxDiff = 0;
            for (ColorHolder c1 : withOverlayPalette.getStream().toList()) {
                for (ColorHolder c2 : withOverlayPalette.getStream().toList()) {
                    double diff = c1.distanceToHybrid(c2);
                    if (diff > maxDiff) maxDiff = diff;
                }
            }

            Palette frontColors = new Palette(Palette.DEFAULT_CUTOFF);
            ArrayList<PostCalcEvent> postQueue = new ArrayList<>();
            //write paletted image base stuff
            for (int x = 0; x < dim; x++) {
                for (int y = 0; y < dim; y++) {
                    ColorHolder b_c = ColorHolder.fromColorInt(b_img.getPixelRGBA(x / bs, y / bs));
                    ColorHolder w_c = ColorHolder.fromColorInt(wImg.getPixelRGBA(x / ws, y / ws));
                    if (backgroundPalette.isInPalette(w_c)) {
                        int w_i = backgroundPalette.closestTo(w_c);
                        int b_i = backgroundPalette.closestTo(b_c);
                        if (w_i != b_i) {
                            pImg.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * w_i)));
                        }
                    } else {
                        //the color sampled isn't in the palette. Now it gets painful...
                        //we could just try dumping it in the overlay, but that isn't going to work too well for some pixels.
                        //let's first find the minimum distance from the palette.
                        int distIndex = backgroundPalette.closestTo(w_c);
                        ColorHolder closestP = backgroundPalette.getColor(distIndex);
                        //Now let's check how close it is.
                        if (closestP.distanceToHybrid(w_c) <= closeCutoff * maxDiff * N_CUTOFF_SCALE) {
                            //Add it to the post-processing queue
                            pImg.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * distIndex)));
                            postQueue.add(new PostCalcEvent(x, y, w_c, closestP.distanceToHybrid(w_c)));
                        } else {
                            //It's too far away. Write to the overlay.
                            oImg.setPixelRGBA(x, y, ColorHolder.toColorInt(w_c));
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
                oImg.close();
                pImg.close();
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

            runPostCalcQueue(oImg, pImg, backgroundPalette, backgroundPaletteSize, frontColors, postQueue);

            trimAndOverlay(dim, ws, oImg, pImg, wImg, backgroundPaletteSize, backgroundPalette);
            if ((trimTrailingPaletteLookup || forceOverlayNeighbors) && fillHoles) {
                int s = pImg.getWidth();
                List<Pair<Integer,Integer>> toSearch = List.of(
                        new Pair<>(0,1),
                        new Pair<>(0,-1),
                        new Pair<>(1,0),
                        new Pair<>(-1,0)
                );
                Map<Pair<Integer,Integer>,Float> alphaMap = new HashMap<>();
                for (int x = 0; x < s; x++) {
                    for (int y = 0; y < s; y++) {
                        ColorHolder overlay = ColorHolder.fromColorInt(oImg.getPixelRGBA(x,y));
                        alphaMap.put(new Pair<>(x,y),overlay.getA());
                        if (overlay.getA()==1 && oImg.getPixelRGBA(x,y)!=wImg.getPixelRGBA(x,y)) {
                            oImg.setPixelRGBA(x,y,ColorHolder.fromColorInt(wImg.getPixelRGBA(x,y)).withA(1f).toInt());
                        }
                    }
                }
                outer:
                while (true) {
                    for (int x = 1; x < s-1; x++) {
                        for (int y = 1; y < s-1; y++) {
                            ColorHolder overlay = ColorHolder.fromColorInt(oImg.getPixelRGBA(x,y));
                            int count = 0;
                            int partialCount = 0;
                            for (Pair<Integer,Integer> is : toSearch) {
                                ColorHolder c = ColorHolder.fromColorInt(oImg.getPixelRGBA(x+is.first(),y+is.last()));
                                if (c.getA()==1) count++;
                                if (c.getA()>0 && alphaMap.get(new Pair<>(x+is.first(),y+is.last()))<1) partialCount++;
                            }
                            ColorHolder origC = ColorHolder.fromColorInt(wImg.getPixelRGBA(x,y));
                            if (overlay.getA()!=1 && (count>=3 || partialCount>=4) && !backgroundPalette.isInPalette(origC,backgroundPalette.getCutoff())) {
                                int orig = wImg.getPixelRGBA(x,y);
                                for (int i = 0; i<s; i++) {
                                    for (int j = 0; j<s; j++) {
                                        int c = wImg.getPixelRGBA(i,j);
                                        if (orig==c) {
                                            oImg.setPixelRGBA(i,j,origC.withA(1.0f).toInt());
                                        }
                                    }
                                }
                                continue outer;
                            }
                        }
                    }
                    break;
                }
            }

            this.overlayImg.set(oImg);
            this.palettedImg.set(pImg);
        }
    }

    private void runPostCalcQueue(NativeImage oImg, NativeImage pImg, Palette backgroundPalette, int backgroundPaletteSize, Palette frontColors, ArrayList<PostCalcEvent> postQueue) {
        for (PostCalcEvent e : postQueue) {
            int x = e.x();
            int y = e.y();
            ColorHolder wColor = e.wColor();
            int fIndex = 0;
            int bIndex = 0;
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
                            fIndex = f;
                            bIndex = b;
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
                            bIndex = backgroundPalette.closestTo(blend);
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
                    fIndex = f;
                    skipO = false;
                }
            }
            pImg.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * bIndex)));
            if (!skipO)
                oImg.setPixelRGBA(x, y, ColorHolder.toColorInt(frontColors.getColor(fIndex).withA(alpha)));
            int frontIndex = frontColors.closestTo(wColor);
            int closeIndex = backgroundPalette.closestTo(wColor);
            if (wColor.distanceToHybrid(backgroundPalette.getColor(closeIndex)) > wColor.distanceToHybrid(frontColors.getColor(frontIndex))) {
                oImg.setPixelRGBA(x, y, ColorHolder.toColorInt(wColor.withA(1.0f)));
                alpha = 1.0f;
            }
            if (alpha >= 1.0f) pImg.setPixelRGBA(x, y, new ColorHolder(0f).withA(0f).toInt());
        }
    }

    public PaletteExtractor fillHoles(boolean fillHoles) {
        this.fillHoles = fillHoles;
        return this;
    }

    private record PostCalcEvent(int x, int y, ColorHolder wColor, double dist) {}

    private record Holder(NativeImage o, NativeImage p, boolean shouldUse) {}
}
