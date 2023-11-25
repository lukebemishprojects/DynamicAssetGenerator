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
import dev.lukebemish.dynamicassetgenerator.impl.util.MultiCloser;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * A source which routes information from several sources into different channels of an output image.
 */
public class ChannelRouteSource implements TexSource {
    public static final Codec<ChannelRouteSource> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.unboundedMap(Codec.STRING, TexSource.CODEC).fieldOf("sources").forGetter(ChannelRouteSource::getSources),
        ChannelSource.CODEC.optionalFieldOf("red").forGetter(s -> Optional.ofNullable(s.getRed())),
        ChannelSource.CODEC.optionalFieldOf("green").forGetter(s -> Optional.ofNullable(s.getGreen())),
        ChannelSource.CODEC.optionalFieldOf("blue").forGetter(s -> Optional.ofNullable(s.getBlue())),
        ChannelSource.CODEC.optionalFieldOf("alpha").forGetter(s -> Optional.ofNullable(s.getAlpha()))
    ).apply(i, (sources, red, green, blue, alpha) -> new ChannelRouteSource(sources, red.orElse(null), green.orElse(null), blue.orElse(null), alpha.orElse(null))));

    private final Map<String, TexSource> sources;
    private final @Nullable ChannelSource red;
    private final @Nullable ChannelSource green;
    private final @Nullable ChannelSource blue;
    private final @Nullable ChannelSource alpha;

    private ChannelRouteSource(Map<String, TexSource> sources, @Nullable ChannelSource red, @Nullable ChannelSource green, @Nullable ChannelSource blue, @Nullable ChannelSource alpha) {
        this.sources = sources;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public static class ChannelSource {
        public static final Codec<ChannelSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("source").forGetter(ChannelSource::getSource),
            Channel.CODEC.fieldOf("channel").forGetter(ChannelSource::getChannel)
        ).apply(i, ChannelSource::new));

        private final String source;
        private final Channel channel;

        private ChannelSource(String source, Channel channel) {
            this.source = source;
            this.channel = channel;
        }

        public Channel getChannel() {
            return channel;
        }

        public String getSource() {
            return source;
        }

        public static class Builder {
            private String source;
            private Channel channel;

            public Builder setSource(String source) {
                this.source = source;
                return this;
            }

            public Builder setChannel(Channel channel) {
                this.channel = channel;
                return this;
            }

            public ChannelSource build() {
                Objects.requireNonNull(source);
                Objects.requireNonNull(channel);
                return new ChannelSource(source, channel);
            }
        }
    }

    public Map<String, TexSource> getSources() {
        return sources;
    }

    public @Nullable ChannelSource getRed() {
        return red;
    }

    public @Nullable ChannelSource getGreen() {
        return green;
    }

    public @Nullable ChannelSource getBlue() {
        return blue;
    }

    @Override
    public @NonNull Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        Map<String, IoSupplier<NativeImage>> inputs = new HashMap<>();
        for (Map.Entry<String, TexSource> entry : this.sources.entrySet()) {
            IoSupplier<NativeImage> input = entry.getValue().getCachedSupplier(data, context);
            if (input == null) {
                data.getLogger().error("Texture given was nonexistent...\n{}", entry.getValue().stringify());
                return null;
            }
            inputs.put(entry.getKey(), input);
        }
        if (red != null && !inputs.containsKey(red.getSource())) {
            data.getLogger().error("Red channel source given was nonexistent: {}", red.getSource());
            return null;
        } else if (green != null && !inputs.containsKey(green.getSource())) {
            data.getLogger().error("Green channel source given was nonexistent: {}", green.getSource());
            return null;
        } else if (blue != null && !inputs.containsKey(blue.getSource())) {
            data.getLogger().error("Blue channel source given was nonexistent: {}", blue.getSource());
            return null;
        } else if (alpha != null && !inputs.containsKey(alpha.getSource())) {
            data.getLogger().error("Alpha channel source given was nonexistent: {}", alpha.getSource());
            return null;
        }
        return () -> {
            PointwiseOperation.Unary<Integer> rOperation = red == null ? null : red.getChannel().makeOperation();
            PointwiseOperation.Unary<Integer> gOperation = green == null ? null : green.getChannel().makeOperation();
            PointwiseOperation.Unary<Integer> bOperation = blue == null ? null : blue.getChannel().makeOperation();
            PointwiseOperation.Unary<Integer> aOperation = alpha == null ? null : alpha.getChannel().makeOperation();

            int running = 0;
            int aIdx, rIdx, gIdx, bIdx;
            if (alpha != null) aIdx = running++; else aIdx = -1;
            if (red != null) rIdx = running++; else rIdx = -1;
            if (green != null) gIdx = running++; else gIdx = -1;
            if (blue != null) bIdx = running; else bIdx = -1;

            PointwiseOperation.Any<Integer> operation = (cs, is) -> {
                int aNew = aIdx == -1 ? 0xFF : aOperation.apply(cs[aIdx], is[aIdx]);
                int rNew = rIdx == -1 ? 0 : rOperation.apply(cs[rIdx], is[rIdx]);
                int gNew = gIdx == -1 ? 0 : gOperation.apply(cs[gIdx], is[gIdx]);
                int bNew = bIdx == -1 ? 0 : bOperation.apply(cs[bIdx], is[bIdx]);
                return ColorTypes.ARGB32.color(aNew, rNew, gNew, bNew);
            };

            var aImg = alpha == null ? null : inputs.get(alpha.getSource()).get();
            var rImg = red == null ? null : inputs.get(red.getSource()).get();
            var gImg = green == null ? null : inputs.get(green.getSource()).get();
            var bImg = blue == null ? null : inputs.get(blue.getSource()).get();

            var images = new ArrayList<NativeImage>();
            if (aImg != null) images.add(aImg);
            if (rImg != null) images.add(rImg);
            if (gImg != null) images.add(gImg);
            if (bImg != null) images.add(bImg);

            try (MultiCloser ignored = new MultiCloser(images)) {
                return ImageUtils.generateScaledImage(operation, images);
            }
        };
    }

    public @Nullable ChannelSource getAlpha() {
        return alpha;
    }

    public static class Builder {
        private Map<String, TexSource> sources;
        private @Nullable ChannelSource red;
        private @Nullable ChannelSource green;
        private @Nullable ChannelSource blue;
        private @Nullable ChannelSource alpha;

        public Builder setSources(Map<String, TexSource> sources) {
            this.sources = sources;
            return this;
        }

        public Builder setRed(@Nullable ChannelSource red) {
            this.red = red;
            return this;
        }

        public Builder setGreen(@Nullable ChannelSource green) {
            this.green = green;
            return this;
        }

        public Builder setBlue(@Nullable ChannelSource blue) {
            this.blue = blue;
            return this;
        }

        public Builder setAlpha(@Nullable ChannelSource alpha) {
            this.alpha = alpha;
            return this;
        }

        public ChannelRouteSource build() {
            Objects.requireNonNull(sources);
            return new ChannelRouteSource(sources, red, green, blue, alpha);
        }
    }
}
