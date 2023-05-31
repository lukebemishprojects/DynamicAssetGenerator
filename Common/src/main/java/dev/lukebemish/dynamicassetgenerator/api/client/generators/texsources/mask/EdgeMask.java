/*
 * Copyright (C) 2022 Luke Bemish and contributors
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

public record EdgeMask(TexSource source, boolean countOutsideFrame, List<Direction> edges, int cutoff) implements TexSource {
    public static final Codec<EdgeMask> CODEC = RecordCodecBuilder.create(i -> i.group(
            TexSource.CODEC.fieldOf("source").forGetter(EdgeMask::source),
            Codec.BOOL.optionalFieldOf("count_outside_frame",false).forGetter(EdgeMask::countOutsideFrame),
            StringRepresentable.fromEnum(Direction::values).listOf().optionalFieldOf("edges", Arrays.stream(Direction.values()).toList()).forGetter(EdgeMask::edges),
            Codec.INT.optionalFieldOf("cutoff",128).forGetter(EdgeMask::cutoff)
    ).apply(i, EdgeMask::new));

    @Override
    public Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> input = this.source.getSupplier(data, context);
        if (input == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.source);
            return null;
        }
        int[] xs = edges.stream().mapToInt(e->e.x).toArray();
        int[] ys = edges.stream().mapToInt(e->e.y).toArray();
        return () -> {
            try (NativeImage inImg = input.get()) {
                int width = inImg.getWidth();
                int height = inImg.getHeight();
                NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA, width, height, false);
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < width; y++) {
                        boolean isEdge = false;
                        int color = inImg.getPixelRGBA(x,y);
                        if (FastColor.ABGR32.alpha(color) >= cutoff) {
                            for (int i = 0; i < xs.length; i++) {
                                int x1 = xs[i]+x;
                                int y1 = ys[i]+y;
                                if ((countOutsideFrame && (x1 < 0 || y1 < 0 || x1 > width-1 || y1 > width-1)) ||
                                        FastColor.ABGR32.alpha(inImg.getPixelRGBA(x1,y1)) < cutoff)
                                    isEdge = true;
                            }
                        }

                        if (isEdge)
                            out.setPixelRGBA(x,y,0xFFFFFFFF);
                        else
                            out.setPixelRGBA(x,y,0);
                    }
                }
                return out;
            }
        };
    }

    private enum Direction implements StringRepresentable {
        NORTH(0,-1),
        NORTHEAST(1,-1),
        EAST(1,0),
        SOUTHEAST(1,1),
        SOUTH(0,1),
        SOUTHWEST(-1,1),
        WEST(-1,0),
        NORTHWEST(-1,-1);

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
}
