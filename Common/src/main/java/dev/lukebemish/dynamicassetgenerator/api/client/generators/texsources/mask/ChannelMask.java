/*
 * Copyright (C) 2023 Luke Bemish and contributors
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * A {@link TexSource} that extracts a single channel from a texture.
 */
@SuppressWarnings("unused")
public final class ChannelMask implements TexSource {
    public static final Codec<ChannelMask> CODEC = RecordCodecBuilder.create(i -> i.group(
            TexSource.CODEC.fieldOf("source").forGetter(ChannelMask::getSource),
            Channel.CODEC.fieldOf("channel").forGetter(ChannelMask::getChannel)
    ).apply(i, ChannelMask::new));
    private final TexSource source;
    private final Channel channel;

    private ChannelMask(TexSource source, Channel channel) {
        this.source = source;
        this.channel = channel;
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
            PointwiseOperation.Unary<Integer> operation = PointwiseOperation.Unary.chain(
                    channel.makeOperation(),
                    (c, i) -> ((c & 0xFF) << 24) | 0xFFFFFF
            );
            try (NativeImage inImg = input.get()) {
                return ImageUtils.generateScaledImage(operation, List.of(inImg));
            }
        };
    }

    public TexSource getSource() {
        return source;
    }

    public Channel getChannel() {
        return channel;
    }

    public static class Builder {
        private TexSource source;
        private Channel channel;

        /**
         * Sets the input texture.
         */
        public Builder setSource(TexSource source) {
            this.source = source;
            return this;
        }

        /**
         * Sets the channel to extract.
         */
        public Builder setChannel(Channel channel) {
            this.channel = channel;
            return this;
        }

        public ChannelMask build() {
            Objects.requireNonNull(source);
            Objects.requireNonNull(channel);
            return new ChannelMask(source, channel);
        }
    }
}
