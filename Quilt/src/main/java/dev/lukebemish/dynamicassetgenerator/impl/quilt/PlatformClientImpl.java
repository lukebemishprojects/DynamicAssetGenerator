/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.quilt;

import com.google.auto.service.AutoService;
import com.mojang.serialization.Codec;
import dev.lukebemish.dynamicassetgenerator.impl.client.platform.PlatformClient;
import dev.lukebemish.dynamicassetgenerator.impl.mixin.SpriteSourcesAccessor;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.ResourceLocation;

@AutoService(PlatformClient.class)
public class PlatformClientImpl implements PlatformClient {
    @Override
    public void addSpriteSource(ResourceLocation location, Codec<? extends SpriteSource> codec) {
        SpriteSourcesAccessor.invokeRegister(location.toString(), codec);
    }
}
