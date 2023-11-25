/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.mixin;

import com.google.common.collect.BiMap;
import com.mojang.serialization.Codec;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpriteSources.class)
public interface SpriteSourcesAccessor {
    @SuppressWarnings("UnusedReturnValue")
    @Invoker("register")
    static SpriteSourceType invokeRegister(String pName, Codec<? extends SpriteSource> pCodec) {
        throw new AssertionError("Mixin failed to apply");
    }

    @Accessor("TYPES")
    static BiMap<ResourceLocation, SpriteSourceType> getTypes() {
        throw new AssertionError("Mixin failed to apply");
    }
}
