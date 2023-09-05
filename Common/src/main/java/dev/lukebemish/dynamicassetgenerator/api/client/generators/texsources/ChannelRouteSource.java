/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.api.client.image.ImageUtils;
import dev.lukebemish.dynamicassetgenerator.api.colors.Channel;
import dev.lukebemish.dynamicassetgenerator.api.colors.ColorTypes;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ChannelRouteSource implements TexSource {
    public static final Codec<ChannelRouteSource> CODEC = RecordCodecBuilder.create(i -> i.group(
        TexSource.CODEC.fieldOf("source").forGetter(ChannelRouteSource::getSource),
        Channel.CODEC.optionalFieldOf("red").forGetter(s -> Optional.ofNullable(s.getRed())),
        Channel.CODEC.optionalFieldOf("green").forGetter(s -> Optional.ofNullable(s.getGreen())),
        Channel.CODEC.optionalFieldOf("blue").forGetter(s -> Optional.ofNullable(s.getBlue())),
        Channel.CODEC.optionalFieldOf("alpha").forGetter(s -> Optional.ofNullable(s.getAlpha()))
    ).apply(i, (source, red, green, blue, alpha) -> new ChannelRouteSource(source, red.orElse(null), green.orElse(null), blue.orElse(null), alpha.orElse(null))));

    private final TexSource source;
    private final @Nullable Channel red;
    private final @Nullable Channel green;
    private final @Nullable Channel blue;
    private final @Nullable Channel alpha;

    private ChannelRouteSource(TexSource source, @Nullable Channel red, @Nullable Channel green, @Nullable Channel blue, @Nullable Channel alpha) {
        this.source = source;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public TexSource getSource() {
        return source;
    }

    public @Nullable Channel getRed() {
        return red;
    }

    public @Nullable Channel getGreen() {
        return green;
    }

    public @Nullable Channel getBlue() {
        return blue;
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
        return () -> {
            PointwiseOperation.Unary<Integer> rOperation = red == null ? (c, i) -> 0 : red.makeOperation();
            PointwiseOperation.Unary<Integer> gOperation = green == null ? (c, i) -> 0 : green.makeOperation();
            PointwiseOperation.Unary<Integer> bOperation = blue == null ? (c, i) -> 0 : blue.makeOperation();
            PointwiseOperation.Unary<Integer> aOperation = alpha == null ? (c, i) -> 0 : alpha.makeOperation();
            PointwiseOperation.Unary<Integer> operation = (c, i) -> {
                var r = rOperation.apply(c, i);
                var g = gOperation.apply(c, i);
                var b = bOperation.apply(c, i);
                var a = aOperation.apply(c, i);
                return ColorTypes.ARGB32.color(a, r, g, b);
            };
            try (NativeImage inImg = input.get()) {
                return ImageUtils.generateScaledImage(operation, List.of(inImg));
            }
        };
    }

    public @Nullable Channel getAlpha() {
        return alpha;
    }

    public static class Builder {
        private TexSource source;
        private @Nullable Channel red;
        private @Nullable Channel green;
        private @Nullable Channel blue;
        private @Nullable Channel alpha;

        public Builder setSource(TexSource source) {
            this.source = source;
            return this;
        }

        public Builder setRed(@Nullable Channel red) {
            this.red = red;
            return this;
        }

        public Builder setGreen(@Nullable Channel green) {
            this.green = green;
            return this;
        }

        public Builder setBlue(@Nullable Channel blue) {
            this.blue = blue;
            return this;
        }

        public Builder setAlpha(@Nullable Channel alpha) {
            this.alpha = alpha;
            return this;
        }

        public ChannelRouteSource build() {
            Objects.requireNonNull(source);
            return new ChannelRouteSource(source, red, green, blue, alpha);
        }
    }
}
