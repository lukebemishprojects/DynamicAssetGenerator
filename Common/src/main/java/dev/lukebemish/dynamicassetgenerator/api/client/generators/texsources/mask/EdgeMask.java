/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.mask;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.NativeImageHelper;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.FastColor;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A {@link TexSource} that detects edges in the alpha channel of a source.
 */
public final class EdgeMask implements TexSource {
    private static final int DEFAULT_CUTOFF = 128;
    private static final boolean DEFAULT_COUNT_OUTSIDE_FRAME = false;
    private static final List<Direction> DEFAULT_EDGES = Arrays.stream(Direction.values()).toList();

    public static final Codec<EdgeMask> CODEC = RecordCodecBuilder.create(i -> i.group(
            TexSource.CODEC.fieldOf("source").forGetter(EdgeMask::getSource),
            Codec.BOOL.optionalFieldOf("count_outside_frame", DEFAULT_COUNT_OUTSIDE_FRAME).forGetter(EdgeMask::isCountOutsideFrame),
            StringRepresentable.fromEnum(Direction::values).listOf().optionalFieldOf("edges", DEFAULT_EDGES).forGetter(EdgeMask::getEdges),
            Codec.INT.optionalFieldOf("cutoff", DEFAULT_CUTOFF).forGetter(EdgeMask::getCutoff)
    ).apply(i, EdgeMask::new));
    private final TexSource source;
    private final boolean countOutsideFrame;
    private final List<Direction> edges;
    private final int cutoff;

    private EdgeMask(TexSource source, boolean countOutsideFrame, List<Direction> edges, int cutoff) {
        this.source = source;
        this.countOutsideFrame = countOutsideFrame;
        this.edges = edges;
        this.cutoff = cutoff;
    }

    @Override
    public Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> input = this.source.getCachedSupplier(data, context);
        if (input == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.source.stringify());
            return null;
        }
        int[] xs = edges.stream().mapToInt(e -> e.x).toArray();
        int[] ys = edges.stream().mapToInt(e -> e.y).toArray();
        return () -> {
            try (NativeImage inImg = input.get()) {
                int width = inImg.getWidth();
                int height = inImg.getHeight();
                NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA, width, height, false);
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < width; y++) {
                        boolean isEdge = false;
                        int color = inImg.getPixelRGBA(x, y);
                        if (FastColor.ABGR32.alpha(color) >= cutoff) {
                            for (int i = 0; i < xs.length; i++) {
                                int x1 = xs[i] + x;
                                int y1 = ys[i] + y;
                                if ((countOutsideFrame && (x1 < 0 || y1 < 0 || x1 > width - 1 || y1 > width - 1)) ||
                                        FastColor.ABGR32.alpha(inImg.getPixelRGBA(x1, y1)) < cutoff)
                                    isEdge = true;
                            }
                        }

                        if (isEdge)
                            out.setPixelRGBA(x, y, 0xFFFFFFFF);
                        else
                            out.setPixelRGBA(x, y, 0);
                    }
                }
                return out;
            }
        };
    }

    public TexSource getSource() {
        return source;
    }

    public boolean isCountOutsideFrame() {
        return countOutsideFrame;
    }

    public List<Direction> getEdges() {
        return edges;
    }

    public int getCutoff() {
        return cutoff;
    }

    /**
     * Represents the direction of a neighboring pixel in 2D space.
     */
    public enum Direction implements StringRepresentable {
        NORTH(0, -1),
        NORTHEAST(1, -1),
        EAST(1, 0),
        SOUTHEAST(1, 1),
        SOUTH(0, 1),
        SOUTHWEST(-1, 1),
        WEST(-1, 0),
        NORTHWEST(-1, -1);

        public final int x;
        public final int y;

        Direction(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static class Builder {
        private TexSource source;
        private boolean countOutsideFrame = DEFAULT_COUNT_OUTSIDE_FRAME;
        private List<Direction> edges = DEFAULT_EDGES;
        private int cutoff = DEFAULT_CUTOFF;

        /**
         * Sets the input texture.
         */
        public Builder setSource(TexSource source) {
            this.source = source;
            return this;
        }

        /**
         * Sets whether to count pixels outside the frame as opaque. Defaults to false.
         */
        public Builder setCountOutsideFrame(boolean countOutsideFrame) {
            this.countOutsideFrame = countOutsideFrame;
            return this;
        }

        /**
         * Sets the directions to look, relative to opaque pixels, for edges. Defaults to all directions.
         */
        public Builder setEdges(List<Direction> edges) {
            this.edges = edges;
            return this;
        }

        /**
         * Sets the cutoff for what is considered opaque. Defaults to 128.
         */
        public Builder setCutoff(int cutoff) {
            this.cutoff = cutoff;
            return this;
        }

        public EdgeMask build() {
            Objects.requireNonNull(source);
            return new EdgeMask(source, countOutsideFrame, edges, cutoff);
        }
    }
}
