/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.DataResult;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.image.ImageUtils;
import dev.lukebemish.dynamicassetgenerator.api.colors.ColorTypes;
import dev.lukebemish.dynamicassetgenerator.api.colors.Palette;
import dev.lukebemish.dynamicassetgenerator.api.colors.clustering.Cluster;
import dev.lukebemish.dynamicassetgenerator.api.colors.clustering.Clusterer;
import dev.lukebemish.dynamicassetgenerator.api.colors.geometry.ColorCoordinates;
import dev.lukebemish.dynamicassetgenerator.impl.CacheReference;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class ForegroundExtractor implements Closeable {
    private static final int[] X_SAMPLING_ORDER = new int[]{-1, -1, -1, 0, 0, 0, 1, 1, 1};
    private static final int[] Y_SAMPLING_ORDER = new int[]{-1, 0, 1, -1, 0, 1, -1, 0, 1};

    private static final int[] ALPHAS = new int[]{26, 38, 51, 64};

    private static final Map<ResourceLocation, Map<String, CacheReference<OutputHolder>>> MULTI_CACHE = new ConcurrentHashMap<>();

    private boolean fillHoles = false;

    private final boolean[] hasLogged = new boolean[3];
    private final double closeCutoff;

    private final NativeImage background;
    private final NativeImage withOverlay;
    public final Predicate<Palette> extend;
    public final boolean trimTrailingPaletteLookup;
    private final boolean forceOverlayNeighbors;
    private final ResourceLocation cacheName;
    private final DataResult<String> cacheKey;

    private OutputHolder outputHolder;

    private final ColorTypes.ConversionCache32 rgb2labCache = new ColorTypes.ConversionCache32(ColorTypes.CIELAB32::fromARGB32);

    private final ColorCoordinates coordinatesLab = new ColorCoordinates() {
        @Override
        public int getX(int color) {
            return ColorTypes.CIELAB32.lightness(rgb2labCache.convert(color));
        }

        @Override
        public int getY(int color) {
            return ColorTypes.CIELAB32.a(rgb2labCache.convert(color));
        }

        @Override
        public int getZ(int color) {
            return ColorTypes.CIELAB32.b(rgb2labCache.convert(color));
        }
    };

    public ForegroundExtractor(ResourceLocation cacheName, DataResult<String> cacheKey, NativeImage background, NativeImage withOverlay, Predicate<Palette> extend, boolean trimTrailingPaletteLookup, boolean forceOverlayNeighbors, double closeCutoff) {
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
        if (outputHolder != null) {
            outputHolder.close();
            outputHolder = null;
        }
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
        Palette backgroundPalette = ImageUtils.getPalette(background);
        backgroundPalette.extend(this.extend);
        Palette withOverlayPalette = ImageUtils.getPalette(withOverlay);

        Clusterer clusterer = new Clusterer(Clusterer.minimumSpacing(backgroundPalette));
        clusterer.addCluster(new Cluster(backgroundPalette));

        Palette notBackgroundPalette = new Palette();

        for (int color : withOverlayPalette) {
            if (backgroundPalette.contains(color)) continue;
            clusterer.addCluster(new Cluster(color));
            notBackgroundPalette.add(color);
        }

        clusterer.run();

        Palette fColors = new Palette();

        int bgCat = clusterer.getCategory(backgroundPalette.getColor(0));

        for (int color : notBackgroundPalette) {
            if (clusterer.getCategory(color) != bgCat) {
                fColors.add(color);
            }
        }

        if (fColors.isEmpty()) {
            for (int x = 0; x < dim; x++) {
                for (int y = 0; y < dim; y++) {
                    ImageUtils.safeSetPixelABGR(oImg, x, y, 0);
                    int bC = ImageUtils.safeGetPixelARGB(background, x / bs, y / bs);
                    int wC = ImageUtils.safeGetPixelARGB(withOverlay, x / ws, y / ws);
                    int bSample = backgroundPalette.getSample(bC);
                    int wSample = backgroundPalette.getSample(wC);
                    if (wSample != bSample)
                        ImageUtils.safeSetPixelABGR(pImg, x, y, FastColor.ABGR32.color(0xFF, wSample, wSample, wSample));
                    else
                        ImageUtils.safeSetPixelABGR(pImg, x, y, 0);
                }
            }
            if (!hasLogged[0])
                DynamicAssetGenerator.LOGGER.warn("Supplied images for extraction contained no differing colors; only extracting palette shifts");
            hasLogged[0] = true;
            return new Holder(oImg, pImg);
        }

        //write paletted image base stuff
        IntStream.range(0, dim).parallel().forEach(x -> {
            for (int y = 0; y < dim; y++) {
                int bC = ImageUtils.safeGetPixelARGB(background, x / bs, y / bs);
                int wC = ImageUtils.safeGetPixelARGB(withOverlay, x / ws, y / ws);
                if (fColors.contains(wC)) {
                    ImageUtils.safeSetPixelARGB(oImg, x, y, wC | 0xFF000000);
                } else if (backgroundPalette.contains(wC)) {
                    int wSample = backgroundPalette.getSample(wC);
                    int bSample = backgroundPalette.getSample(bC);
                    if (wSample != bSample) {
                        ImageUtils.safeGetPixelABGR(oImg, x, y, FastColor.ABGR32.color(0xFF, wSample, wSample, wSample));
                    }
                } else {
                    int fIndex = 0;
                    int bSample = 0;
                    double lowest = 255*255;
                    int alpha = 0;
                    boolean skipOverlay = false;
                    for (int a : ALPHAS) {
                        for (int b = 0; b < backgroundPalette.size(); b++) {
                            for (int f = 0; f < fColors.size(); f++) {
                                int bColor = backgroundPalette.getColorFromIndex(b);
                                int fColor = fColors.getColorFromIndex(f);
                                int fWithAlpha = (fColor & 0xFFFFFF) | (a << 24);
                                double dist = extractorDistance(ColorTypes.ARGB32.alphaBlend(fWithAlpha, bColor), wC);
                                if (dist < lowest) {
                                    lowest = dist;
                                    alpha = a;
                                    fIndex = f;
                                    bSample = b * 256 / backgroundPalette.size();
                                    skipOverlay = false;
                                }
                            }
                            for (int b1 = 0; b1 < backgroundPalette.size(); b1++) {
                                int bColor = backgroundPalette.getColorFromIndex(b);
                                int fColor = backgroundPalette.getColorFromIndex(b1);
                                int fWithAlpha = (fColor & 0xFFFFFF) | (a << 24);
                                double dist = extractorDistance(ColorTypes.ARGB32.alphaBlend(fWithAlpha, bColor), wC);
                                if (dist < lowest) {
                                    lowest = dist;
                                    alpha = a;
                                    bSample = backgroundPalette.getSample(fWithAlpha);
                                    skipOverlay = true;
                                }
                            }
                        }
                    }
                    for (int f = 0; f < fColors.size(); f++) {
                        int fColor = fColors.getColorFromIndex(f);
                        double dist = extractorDistance(fColor, wC);
                        if (dist < lowest) {
                            lowest = dist;
                            alpha = 255;
                            fIndex = f;
                            skipOverlay = false;
                        }
                    }
                    ImageUtils.safeSetPixelARGB(pImg, x, y, bSample);
                    if (!skipOverlay) {
                        ImageUtils.safeSetPixelARGB(oImg, x, y, (fColors.getColorFromIndex(fIndex) & 0xFFFFFF) | (alpha << 24));
                    }
                    if (alpha >= 255) {
                        ImageUtils.safeSetPixelABGR(oImg, x, y, 0);
                    }
                }
            }
        });

        trimAndOverlay(dim, ws, oImg, pImg, withOverlay, backgroundPalette);
        return new Holder(oImg, pImg);
    }

    private void trimAndOverlay(int dim, int ws, NativeImage oImg, NativeImage pImg, NativeImage wImg, Palette backgroundPalette) {
        if (trimTrailingPaletteLookup || forceOverlayNeighbors) {
            IntStream.range(0, dim).parallel().forEach(x -> {
                for (int y = 0; y < dim; y++) {
                    boolean hasNeighbor = false;
                    boolean hasFullNeighbor = false;
                    for (int j = 0; j < X_SAMPLING_ORDER.length; j++) {
                        int xt = x + X_SAMPLING_ORDER[j];
                        int yt = y + Y_SAMPLING_ORDER[j];
                        if (0 <= xt && xt < dim && 0 <= yt && yt < dim) {
                            int alpha = FastColor.ABGR32.alpha(ImageUtils.safeGetPixelABGR(oImg, xt, yt));
                            if (alpha != 0) {
                                hasNeighbor = true;
                            }
                            if (alpha >= 255) {
                                hasFullNeighbor = true;
                            }
                        }
                    }
                    if (trimTrailingPaletteLookup && !hasNeighbor) {
                        ImageUtils.safeSetPixelABGR(pImg, x, y, 0);
                    }
                    if (forceOverlayNeighbors && hasFullNeighbor && FastColor.ABGR32.alpha(ImageUtils.safeGetPixelABGR(oImg, x, y)) == 0) {
                        int wColor = ImageUtils.safeGetPixelARGB(wImg, x / ws, y / ws);
                        int wSample = backgroundPalette.getSample(wColor);
                        ImageUtils.safeSetPixelABGR(pImg, x, y, FastColor.ABGR32.color(255, wSample, wSample, wSample));
                    }
                }
            });
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
        Palette backgroundPalette = ImageUtils.getPalette(background);
        backgroundPalette.extend(this.extend);
        Palette withOverlayPalette = ImageUtils.getPalette(withOverlay);
        withOverlayPalette.extend(this.extend);

        double workingAvgDiff = 0;
        int avgCount = 0;
        for (int c1 : backgroundPalette) {
            for (int c2 : backgroundPalette) {
                workingAvgDiff += extractorDistance(c1, c2);
                avgCount++;
            }
        }
        if (avgCount > 0) {
            workingAvgDiff /= avgCount;
        }
        final double avgDiff = workingAvgDiff;

        Palette frontColors = new Palette();

        ArrayList<PostCalcEvent> postQueue = new ArrayList<>();
        //write paletted image base stuff
        IntStream.range(0, dim).parallel().forEach(x -> {
            for (int y = 0; y < dim; y++) {
                int bColor = ImageUtils.safeGetPixelARGB(background, x / bs, y / bs);
                int wColor = ImageUtils.safeGetPixelARGB(withOverlay, x / ws, y / ws);
                if (backgroundPalette.contains(wColor)) {
                    int wSample = backgroundPalette.getSample(wColor);
                    int bSample = backgroundPalette.getSample(bColor);
                    if (wSample != bSample) {
                        ImageUtils.safeSetPixelABGR(pImg, x, y, FastColor.ABGR32.color(255, wSample, wSample, wSample));
                    }
                } else {
                    //the color sampled isn't in the palette. Now it gets painful...
                    //we could just try dumping it in the overlay, but that isn't going to work too well for some pixels.
                    //let's first find the minimum distance from the palette.
                    int closestSample = backgroundPalette.getSample(wColor);
                    //Now let's check how close it is.
                    if (backgroundPalette.distanceToPolyLine(wColor, coordinatesLab) < closeCutoff * avgDiff) {
                        //Add it to the post-processing queue
                        ImageUtils.safeSetPixelABGR(pImg, x, y, FastColor.ABGR32.color(0xFF, closestSample, closestSample, closestSample));
                        synchronized (postQueue) {
                            postQueue.add(new PostCalcEvent(x, y, wColor));
                        }
                    } else {
                        //It's too far away. Write to the overlay.
                        ImageUtils.safeSetPixelARGB(oImg, x, y, wColor);
                        synchronized (frontColors) {
                            frontColors.add(wColor);
                        }
                    }
                }
            }
        });

        if (frontColors.isEmpty() || frontColors.size()*backgroundPalette.size()*postQueue.size() > DynamicAssetGenerator.getConfig().paletteForceClusteringCutoff()) {
            Holder alt = recalcImagesAlternate();
            if (frontColors.isEmpty()) {
                if (!hasLogged[1])
                    DynamicAssetGenerator.LOGGER.warn("Supplied images for extraction contained few differing colors; attempting clustering color extraction.");
                hasLogged[1] = true;
            } else {
                if (!hasLogged[2])
                    DynamicAssetGenerator.LOGGER.warn("Supplied images for extraction contained too many colors and were too high resolution to resolve post-calculation queue; attempting clustering color extraction.");
                hasLogged[2] = true;
            }
            this.outputHolder = new OutputHolder(alt.o(), alt.p());
            oImg.close();
            pImg.close();
            return;
        }

        runPostCalcQueue(oImg, pImg, backgroundPalette, frontColors, postQueue);

        trimAndOverlay(dim, ws, oImg, pImg, withOverlay, backgroundPalette);

        if ((trimTrailingPaletteLookup || forceOverlayNeighbors) && fillHoles) {
            int s = pImg.getWidth();
            Long2IntMap alphaMap = new Long2IntOpenHashMap();
            for (int x = 0; x < s; x++) {
                for (int y = 0; y < s; y++) {
                    int overlay = ImageUtils.safeGetPixelABGR(oImg, x, y);
                    int alpha = FastColor.ABGR32.alpha(overlay);
                    alphaMap.put(pair(x, y), alpha);
                    int wOverlayColor = ImageUtils.safeGetPixelABGR(withOverlay, x / ws, y / ws);
                    if (alpha == 255 && wOverlayColor != overlay) {
                        ImageUtils.safeSetPixelABGR(oImg, x, y, wOverlayColor | 0xFF000000);
                    }
                }
            }

            outer:
            while (true) {
                for (int x = 1; x < s - 1; x++) {
                    for (int y = 1; y < s - 1; y++) {
                        int overlay = ImageUtils.safeGetPixelARGB(oImg, x, y);
                        int count = 0;
                        int partialCount = 0;
                        for (int idx = 0; idx < TO_SEARCH_XS.length; idx++) {
                            int xOff = TO_SEARCH_XS[idx];
                            int yOff = TO_SEARCH_YS[idx];
                            int c = ImageUtils.safeGetPixelARGB(oImg, x + xOff, y + yOff);
                            int alpha = FastColor.ARGB32.alpha(c);
                            if (alpha == 0xFF) count++;
                            if (alpha > 0 && alphaMap.get(pair(x + xOff, y + yOff)) < 255)
                                partialCount++;
                        }
                        int origC = ImageUtils.safeGetPixelARGB(withOverlay, x / ws, y / ws);
                        if (FastColor.ARGB32.alpha(overlay) != 255 && (count >= 3 || partialCount >= 4) && !backgroundPalette.contains(origC)) {
                            int orig = ImageUtils.safeGetPixelABGR(withOverlay, x, y);
                            for (int i = 0; i < s; i++) {
                                for (int j = 0; j < s; j++) {
                                    int c = ImageUtils.safeGetPixelABGR(withOverlay, i, j);
                                    if (orig == c) {
                                        ImageUtils.safeSetPixelARGB(oImg, i, j, origC | 0xFF000000);
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

    private static final int[] TO_SEARCH_XS = new int[] {0, 1, 0, -1};
    private static final int[] TO_SEARCH_YS = new int[] {1, 0, -1, 0};

    private static long pair(int a, int b) {
        return (((long)a) << 32) | (b & 0xffffffffL);
    }

    private double extractorDistance(int color1, int color2) {
        int c1Lab = rgb2labCache.convert(color1);
        int c2Lab = rgb2labCache.convert(color2);
        return ColorTypes.CIELAB32.distance(c1Lab, c2Lab);
    }

    private void runPostCalcQueue(NativeImage oImg, NativeImage pImg, Palette backgroundPalette, Palette frontColors, List<PostCalcEvent> postQueue) {
        postQueue.parallelStream().forEach(e -> {
            int x = e.x();
            int y = e.y();
            int wColor = e.wColor();

            if (backgroundPalette.distanceToPolyLine(wColor, coordinatesLab) > frontColors.distanceToPolyLine(wColor, coordinatesLab)) {
                ImageUtils.safeSetPixelARGB(oImg, x, y, wColor | 0xFF000000);
                ImageUtils.safeSetPixelARGB(pImg, x, y, 0);
                return;
            }

            int fIndex = 0;
            int bSample = 0;
            double lowest = 255*255;
            int alpha = 0;
            boolean skipOverlay = false;
            for (int a : ALPHAS) {
                for (int b = 0; b < backgroundPalette.size(); b++) {
                    int bColor = backgroundPalette.getColorFromIndex(b);
                    for (int f = 0; f < frontColors.size(); f++) {
                        int fColor = frontColors.getColorFromIndex(f);
                        double dist = extractorDistance(wColor, ColorTypes.ARGB32.alphaBlend((fColor & 0xFFFFFF) | (a << 24), bColor));
                        if (dist < lowest) {
                            lowest = dist;
                            alpha = a;
                            fIndex = f;
                            bSample = b * 256 / backgroundPalette.size();
                            skipOverlay = false;
                        }
                    }
                    for (int b1 = 0; b1 < backgroundPalette.size(); b1++) {
                        int fColor = backgroundPalette.getColorFromIndex(b1);
                        int blend = ColorTypes.ARGB32.alphaBlend((fColor & 0xFFFFFF) | (a << 24), bColor);
                        double dist = extractorDistance(wColor, blend);
                        if (dist < lowest) {
                            lowest = dist;
                            alpha = a;
                            bSample = backgroundPalette.getSample(blend);
                            skipOverlay = true;
                        }
                    }
                }
            }
            for (int f = 0; f < frontColors.size(); f++) {
                int fColor = frontColors.getColorFromIndex(f);
                double dist = extractorDistance(wColor, fColor);
                if (dist < lowest) {
                    lowest = dist;
                    alpha = 255;
                    fIndex = f;
                    skipOverlay = false;
                }
            }
            ImageUtils.safeSetPixelABGR(pImg, x, y, FastColor.ABGR32.color(255, bSample, bSample, bSample));
            if (!skipOverlay) {
                int overlayColor = (frontColors.getColorFromIndex(fIndex) & 0xFFFFFF) | (alpha << 24);
                ImageUtils.safeSetPixelARGB(oImg, x, y, overlayColor);
            }
            if (alpha == 255)
                ImageUtils.safeSetPixelARGB(pImg, x, y, 0);
        });
    }

    public ForegroundExtractor fillHoles(boolean fillHoles) {
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

    private record PostCalcEvent(int x, int y, int wColor) {}

    private record Holder(NativeImage o, NativeImage p) implements Closeable {
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
            if ((cache = MULTI_CACHE.get(context.getCacheName())) != null) {
                cache.forEach((s, e) -> e.getHeld().close());
                MULTI_CACHE.remove(context.getCacheName());
            }
        }
    }
}
