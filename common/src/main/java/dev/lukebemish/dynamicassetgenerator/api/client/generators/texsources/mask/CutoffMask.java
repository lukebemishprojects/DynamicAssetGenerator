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
import dev.lukebemish.dynamicassetgenerator.api.client.image.ImageUtils;
import dev.lukebemish.dynamicassetgenerator.api.colors.Channel;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * A {@link TexSource} that extracts the locations where a given channel is above or below a given cutoff.
 */
public final class CutoffMask implements TexSource {
    private static final int DEFAULT_CUTOFF = 128;
    private static final Channel DEFAULT_CHANNEL = Channel.ALPHA;

    public static final Codec<CutoffMask> CODEC = RecordCodecBuilder.create(i -> i.group(
            Channel.CODEC.optionalFieldOf("channel", DEFAULT_CHANNEL).forGetter(CutoffMask::getChannel),
            TexSource.CODEC.fieldOf("source").forGetter(CutoffMask::getSource),
            Codec.INT.optionalFieldOf("cutoff", DEFAULT_CUTOFF).forGetter(CutoffMask::getCutoff)
    ).apply(i, CutoffMask::new));
    private final Channel channel;
    private final TexSource source;
    private final int cutoff;


    private CutoffMask(Channel channel, TexSource source, int cutoff) {
        this.channel = channel;
        this.source = source;
        this.cutoff = cutoff;
    }

    @Override
    public @NonNull Codec<? extends TexSource> codec() {
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
            PointwiseOperation.Unary<Integer> operation = PointwiseOperation.Unary.chain(
                    channel.makeOperation(),
                    (c, i) -> i ? ((c & 0xFF) >= cutoff ? 0xFFFFFFFF : 0) : 0
            );
            try (NativeImage inImg = input.get()) {
                return ImageUtils.generateScaledImage(operation, List.of(inImg));
            }
        };
    }

    public Channel getChannel() {
        return channel;
    }

    public TexSource getSource() {
        return source;
    }

    public int getCutoff() {
        return cutoff;
    }

    public static class Builder {
        private Channel channel = DEFAULT_CHANNEL;
        private TexSource source;
        private int cutoff = DEFAULT_CUTOFF;

        /**
         * Sets the channel to inspect.
         */
        public Builder setChannel(Channel channel) {
            this.channel = channel;
            return this;
        }

        /**
         * Sets the input texture.
         */
        public Builder setSource(TexSource source) {
            this.source = source;
            return this;
        }

        /**
         * Sets the cutoff value for the channel. Should be between 0 and 255.
         */
        public Builder setCutoff(int cutoff) {
            this.cutoff = cutoff;
            return this;
        }

        public CutoffMask build() {
            Objects.requireNonNull(source);
            return new CutoffMask(channel, source, cutoff);
        }
    }
}
