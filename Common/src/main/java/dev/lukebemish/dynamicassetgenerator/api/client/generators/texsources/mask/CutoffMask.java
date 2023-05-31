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
import dev.lukebemish.dynamicassetgenerator.api.client.image.ImageUtils;
import dev.lukebemish.dynamicassetgenerator.api.colors.Channel;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record CutoffMask(Channel channel, TexSource source, int cutoff) implements TexSource {
    public static final Codec<CutoffMask> CODEC = RecordCodecBuilder.create(i->i.group(
            Channel.CODEC.optionalFieldOf("channel",Channel.ALPHA).forGetter(CutoffMask::channel),
            TexSource.CODEC.fieldOf("source").forGetter(CutoffMask::source),
            Codec.INT.optionalFieldOf("cutoff",128).forGetter(CutoffMask::cutoff)
    ).apply(i,CutoffMask::new));

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
        return () -> {
            PointwiseOperation.Unary<Integer> operation = PointwiseOperation.Unary.chain(
                    channel.makeOperation(),
                    (c, i) -> i ? (c >= cutoff ? 0xFFFFFFFF : 0) : 0
            );
            try (NativeImage inImg = input.get()) {
                return ImageUtils.generateScaledImage(operation, List.of(inImg));
            }
        };
    }
}
