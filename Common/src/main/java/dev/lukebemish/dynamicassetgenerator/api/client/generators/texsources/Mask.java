/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.api.client.image.ImageUtils;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.Operations;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record Mask(ITexSource input, ITexSource mask) implements ITexSource {
    public static final Codec<Mask> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.CODEC.fieldOf("input").forGetter(Mask::input),
            ITexSource.CODEC.fieldOf("mask").forGetter(Mask::mask)
    ).apply(instance, Mask::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> input = this.input().getSupplier(data, context);
        IoSupplier<NativeImage> mask = this.mask().getSupplier(data, context);

        if (input == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.mask());
            return null;
        }
        if (mask == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.input());
            return null;
        }

        return () -> {
            try (NativeImage inImg = input.get();
                 NativeImage maskImg = mask.get()) {

                return ImageUtils.generateScaledImage(Operations.MASK, List.of(inImg, maskImg));
            }
        };
    }
}
