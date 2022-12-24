/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.impl.CacheReference;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.client.palette.ColorHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.palette.Palette;
import dev.lukebemish.dynamicassetgenerator.impl.client.util.Clusterer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PaletteExtractor implements Closeable {
    private static final int[] X_SAMPLING_ORDER = new int[]{-1, -1, -1, 0, 0, 0, 1, 1, 1};
    private static final int[] Y_SAMPLING_ORDER = new int[]{-1, 0, 1, -1, 0, 1, -1, 0, 1};

    private static final Map<ResourceLocation, Map<String, CacheReference<OutputHolder>>> MULTI_CACHE = new ConcurrentHashMap<>();

    private boolean fillHoles =false;

    private final boolean[] hasLogged = new boolean[2];
    private final double closeCutoff;

    private final NativeImage background;
    private final NativeImage withOverlay;
    public final int extend;
    public final boolean trimTrailingPaletteLookup;
    private final boolean forceOverlayNeighbors;
    private final ResourceLocation cacheName;
    private final DataResult<String> cacheKey;

    private OutputHolder outputHolder;

    private static final float N_CUTOFF_SCALE = 1.5f;

    public PaletteExtractor(ResourceLocation cacheName, DataResult<String> cacheKey, NativeImage background, NativeImage withOverlay, int extend, boolean trimTrailingPaletteLookup, boolean forceOverlayNeighbors, double closeCutoff) {
        this.cacheName = cacheName;
        this.cacheKey = cacheKey;
        this.background = background;
        this.withOverlay = withOverlay;
        this.extend = extend;
        this.trimTrailingPaletteLookup = trimTrailingPaletteLookup;
        this.forceOverlayNeighbors = forceOverlayNeighbors;
        this.closeCutoff = closeCutoff;
    }

    public NativeImage getOverlayImg() {
        return outputHolder.o;
    }

    public NativeImage getPalettedImg() {
        return outputHolder.p;
    }

    private void tryCloseOutputs() {
        outputHolder.close();
        outputHolder = null;
    }

    private Holder recalcImagesAlternate() {
        int bDim = Math.min(background.getHeight(), background.getWidth());
        int wDim = Math.min(withOverlay.getHeight(), withOverlay.getWidth());
        int dim = Math.max(bDim, wDim);
        int bs = dim / bDim;
        int ws = dim / wDim;
        //Assemble palette for background
        NativeImage oImg = NativeImageHelper.of(NativeImage.Format.RGBA, dim, dim, false);
        NativeImage pImg = NativeImageHelper.of(NativeImage.Format.RGBA, dim, dim, false);
        Palette backgroundPalette = Palette.extractPalette(background, extend);
        Palette withOverlayPalette = Palette.extractPalette(withOverlay, 0);
        int backgroundPaletteSize = backgroundPalette.getSize();

        Palette nbColors = new Palette();
        withOverlayPalette.getStream().forEach(c -> {
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
                    oImg.setPixelRGBA(x, y, 0);
                    ColorHolder bC = ColorHolder.fromColorInt(background.getPixelRGBA(x / bs, y / bs));
                    ColorHolder wC = ColorHolder.fromColorInt(withOverlay.getPixelRGBA(x / ws, y / ws));
                    int wI = backgroundPalette.closestTo(wC);
                    int bI = backgroundPalette.closestTo(bC);
                    if (wI != bI)
                        pImg.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * wI)));
                    else pImg.setPixelRGBA(x, y, 0);
                }
            }
            if (!hasLogged[0])
                DynamicAssetGenerator.LOGGER.warn("Supplied images for extraction contained no differing colors; only extracting palette shifts");
            hasLogged[0] = true;
            return new Holder(oImg, pImg, false);
        }

        //write paletted image base stuff
        for (int x = 0; x < dim; x++) {
            for (int y = 0; y < dim; y++) {
                ColorHolder bC = ColorHolder.fromColorInt(background.getPixelRGBA(x / bs, y / bs));
                ColorHolder wC = ColorHolder.fromColorInt(withOverlay.getPixelRGBA(x / ws, y / ws));
                if (fColors.isInPalette(wC)) {
                    oImg.setPixelRGBA(x, y, ColorHolder.toColorInt(wC.withA(1)));
                } else if (backgroundPalette.isInPalette(wC)) {
                    int wI = backgroundPalette.closestTo(wC);
                    int bI = backgroundPalette.closestTo(bC);
                    if (wI != bI) {
                        pImg.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * wI)));
                    }
                } else {
                    int fIndex = 0;
                    int bIndex = 0;
                    double lowest = 200f;
                    float alpha = 0f;
                    boolean skipO = false;
                    for (float a = 0.1f; a <= 0.25f; a += 0.05) {
                        for (int b = 0; b < backgroundPaletteSize; b++) {
                            for (int f = 0; f < fColors.getSize(); f++) {
                                ColorHolder bColor = backgroundPalette.getColor(b);
                                ColorHolder fColor = fColors.getColor(f);
                                double dist = wC.distanceToLab(ColorHolder.alphaBlend(fColor.withA(a), bColor));
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
                                double dist = wC.distanceToLab(blend);
                                if (dist < lowest) {
                                    lowest = dist;
                                    alpha = a;
                                    bIndex = backgroundPalette.closestTo(blend);
                                    skipO = true;
                                }
                            }
                        }
                    }
                    for (int f = 0; f < fColors.getSize(); f++) {
                        ColorHolder fColor = fColors.getColor(f);
                        double dist = wC.distanceToLab(fColor);
                        if (dist < lowest) {
                            lowest = dist;
                            alpha = 1.0f;
                            fIndex = f;
                            skipO = false;
                        }
                    }
                    pImg.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * bIndex)));
                    if (!skipO) oImg.setPixelRGBA(x, y, fColors.getColor(fIndex).withA(alpha).toInt());
                    if (alpha >= 1.0f) pImg.setPixelRGBA(x, y, new ColorHolder(0f).withA(0f).toInt());
                }
            }
        }

        trimAndOverlay(dim, ws, oImg, pImg, withOverlay, backgroundPaletteSize, backgroundPalette);
        return new Holder(oImg, pImg, nbColors.dist(nbColors) < nbColors.dist(backgroundPalette));
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

    private void recalcImages() {
        int bDim = Math.min(background.getHeight(), background.getWidth());
        int wDim = Math.min(withOverlay.getHeight(), withOverlay.getWidth());
        int dim = Math.max(bDim, wDim);
        int bs = dim / bDim;
        int ws = dim / wDim;
        //Assemble palette for background
        NativeImage oImg = NativeImageHelper.of(NativeImage.Format.RGBA, dim, dim, false);
        NativeImage pImg = NativeImageHelper.of(NativeImage.Format.RGBA, dim, dim, false);
        Palette backgroundPalette = Palette.extractPalette(background, extend);
        Palette withOverlayPalette = Palette.extractPalette(withOverlay, extend);
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
                ColorHolder bC = ColorHolder.fromColorInt(background.getPixelRGBA(x / bs, y / bs));
                ColorHolder wC = ColorHolder.fromColorInt(withOverlay.getPixelRGBA(x / ws, y / ws));
                if (backgroundPalette.isInPalette(wC)) {
                    int wI = backgroundPalette.closestTo(wC);
                    int bI = backgroundPalette.closestTo(bC);
                    if (wI != bI) {
                        pImg.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * wI)));
                    }
                } else {
                    //the color sampled isn't in the palette. Now it gets painful...
                    //we could just try dumping it in the overlay, but that isn't going to work too well for some pixels.
                    //let's first find the minimum distance from the palette.
                    int distIndex = backgroundPalette.closestTo(wC);
                    ColorHolder closestP = backgroundPalette.getColor(distIndex);
                    //Now let's check how close it is.
                    if (closestP.distanceToHybrid(wC) <= closeCutoff * maxDiff * N_CUTOFF_SCALE) {
                        //Add it to the post-processing queue
                        pImg.setPixelRGBA(x, y, ColorHolder.toColorInt(new ColorHolder(1f / (backgroundPaletteSize - 1) * distIndex)));
                        postQueue.add(new PostCalcEvent(x, y, wC, closestP.distanceToHybrid(wC)));
                    } else {
                        //It's too far away. Write to the overlay.
                        oImg.setPixelRGBA(x, y, ColorHolder.toColorInt(wC));
                        frontColors.tryAdd(wC);
                    }
                }
            }
        }

        Holder alt = recalcImagesAlternate();
        if (frontColors.getSize() == 0) {
            if (!hasLogged[1])
                DynamicAssetGenerator.LOGGER.warn("Supplied images for extraction contained few differing colors; attempting clustering color extraction.");
            hasLogged[1] = true;
            this.outputHolder = new OutputHolder(alt.o(), alt.p());
            oImg.close();
            pImg.close();
            return;
        } else if (frontColors.getSize()*backgroundPaletteSize*postQueue.size() > DynamicAssetGenerator.getConfig().paletteForceClusteringCutoff()) {
            if (!hasLogged[1])
                DynamicAssetGenerator.LOGGER.warn("Supplied images for extraction contained too many colors and were too high resolution to resolve post-calculation queue; attempting clustering color extraction.");
            hasLogged[1] = true;
            this.outputHolder = new OutputHolder(alt.o(), alt.p());
            oImg.close();
            pImg.close();
            return;
        }
        alt.close();
        /*else if (alt.shouldUse()) {
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

        trimAndOverlay(dim, ws, oImg, pImg, withOverlay, backgroundPaletteSize, backgroundPalette);
        if ((trimTrailingPaletteLookup || forceOverlayNeighbors) && fillHoles) {
            int s = pImg.getWidth();
            List<Pair<Integer, Integer>> toSearch = List.of(
                    new Pair<>(0, 1),
                    new Pair<>(0, -1),
                    new Pair<>(1, 0),
                    new Pair<>(-1, 0)
            );
            Map<Pair<Integer, Integer>, Float> alphaMap = new HashMap<>();
            for (int x = 0; x < s; x++) {
                for (int y = 0; y < s; y++) {
                    ColorHolder overlay = ColorHolder.fromColorInt(oImg.getPixelRGBA(x, y));
                    alphaMap.put(new Pair<>(x, y), overlay.getA());
                    if (overlay.getA() == 1 && oImg.getPixelRGBA(x, y) != withOverlay.getPixelRGBA(x, y)) {
                        oImg.setPixelRGBA(x, y, ColorHolder.fromColorInt(withOverlay.getPixelRGBA(x, y)).withA(1f).toInt());
                    }
                }
            }
            outer:
            while (true) {
                for (int x = 1; x < s - 1; x++) {
                    for (int y = 1; y < s - 1; y++) {
                        ColorHolder overlay = ColorHolder.fromColorInt(oImg.getPixelRGBA(x, y));
                        int count = 0;
                        int partialCount = 0;
                        for (Pair<Integer, Integer> is : toSearch) {
                            ColorHolder c = ColorHolder.fromColorInt(oImg.getPixelRGBA(x + is.getFirst(), y + is.getSecond()));
                            if (c.getA() == 1) count++;
                            if (c.getA() > 0 && alphaMap.get(new Pair<>(x + is.getFirst(), y + is.getSecond())) < 1)
                                partialCount++;
                        }
                        ColorHolder origC = ColorHolder.fromColorInt(withOverlay.getPixelRGBA(x, y));
                        if (overlay.getA() != 1 && (count >= 3 || partialCount >= 4) && !backgroundPalette.isInPalette(origC, backgroundPalette.getCutoff())) {
                            int orig = withOverlay.getPixelRGBA(x, y);
                            for (int i = 0; i < s; i++) {
                                for (int j = 0; j < s; j++) {
                                    int c = withOverlay.getPixelRGBA(i, j);
                                    if (orig == c) {
                                        oImg.setPixelRGBA(i, j, origC.withA(1.0f).toInt());
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

        this.outputHolder = new OutputHolder(oImg, pImg);
    }

    private void runPostCalcQueue(NativeImage oImg, NativeImage pImg, Palette backgroundPalette, int backgroundPaletteSize, Palette frontColors, List<PostCalcEvent> postQueue) {
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

    @Override
    public void close() {
        tryCloseOutputs();
        if (background!=null)
            background.close();
        if (withOverlay!=null)
            withOverlay.close();
    }

    private record PostCalcEvent(int x, int y, ColorHolder wColor, double dist) {}

    private record Holder(NativeImage o, NativeImage p, boolean shouldUse) implements Closeable {
        @Override
        public void close() {
            o.close();
            p.close();
        }
    }

    private record OutputHolder(@Nullable NativeImage o, @Nullable NativeImage p) implements Closeable {
        public OutputHolder copy() {
            NativeImage newO = null;
            if (o != null) {
                newO = NativeImageHelper.of(o.format(), o.getWidth(), o.getHeight(), false);
                newO.copyFrom(o);
            }
            NativeImage newP = null;
            if (p != null) {
                newP = NativeImageHelper.of(p.format(), p.getWidth(), p.getHeight(), false);
                newP.copyFrom(p);
            }
            return new OutputHolder(newO, newP);
        }

        @Override
        public void close() {
            if (p!=null) {
                p.close();
            }
            if (o!=null) {
                o.close();
            }
        }
    }


    public void unCacheOrReCalc() {
        if (this.cacheKey.result().isEmpty()) {
            this.recalcImages();
            return;
        }
        var cache = MULTI_CACHE.computeIfAbsent(this.cacheName, k -> new ConcurrentHashMap<>());
        var ref = cache.computeIfAbsent(cacheKey.result().get(), k -> new CacheReference<>());
        ref.doSync(holder -> {
            if (holder != null) {
                this.outputHolder = holder.copy();
            } else {
                this.recalcImages();
                ref.setHeld(this.outputHolder.copy());
            }
        });
    }

    public static void reset(ResourceGenerationContext context) {
        synchronized (MULTI_CACHE) {
            Map<String, CacheReference<OutputHolder>> cache;
            if ((cache = MULTI_CACHE.get(context.cacheName())) != null) {
                cache.forEach((s, e) -> {
                    e.getHeld().close();
                });
                MULTI_CACHE.remove(context.cacheName());
            }
        }
    }
}
