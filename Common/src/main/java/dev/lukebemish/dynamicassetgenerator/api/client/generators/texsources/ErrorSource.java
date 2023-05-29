/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

public record ErrorSource(String message) implements TexSource {
    public static final Codec<ErrorSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("message").forGetter(ErrorSource::message)
    ).apply(instance, ErrorSource::new));

    @Override
    public Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        data.getLogger().error(message());
        return null;
    }
}
