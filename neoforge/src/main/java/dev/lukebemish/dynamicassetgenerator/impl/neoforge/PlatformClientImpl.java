/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.neoforge;

import com.google.auto.service.AutoService;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import dev.lukebemish.dynamicassetgenerator.impl.client.platform.PlatformClient;
import dev.lukebemish.dynamicassetgenerator.impl.mixin.SpriteSourcesAccessor;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.util.ArrayList;
import java.util.List;

@AutoService(PlatformClient.class)
public class PlatformClientImpl implements PlatformClient {
    private static final List<Pair<ResourceLocation, Codec<? extends SpriteSource>>> SPRITE_SOURCE_QUEUE = new ArrayList<>();
    private static boolean SPRITE_SOURCES_REGISTERED = false;


    public void addSpriteSource(ResourceLocation location, Codec<? extends SpriteSource> codec) {
        if (SPRITE_SOURCES_REGISTERED) {
            throw new IllegalStateException("Sprite sources have already been registered. Try registering yours during mod initialization!");
        }
        SPRITE_SOURCE_QUEUE.add(Pair.of(location, codec));
    }

    public static void reloadListenerListener(RegisterClientReloadListenersEvent event) {
        if (SPRITE_SOURCES_REGISTERED) return;
        SPRITE_SOURCES_REGISTERED = true;
        for (var pair : SPRITE_SOURCE_QUEUE) {
            SpriteSourcesAccessor.invokeRegister(pair.getFirst().toString(), pair.getSecond());
        }
    }
}
