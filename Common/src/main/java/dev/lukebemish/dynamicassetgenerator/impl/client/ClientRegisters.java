/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import dev.lukebemish.dynamicassetgenerator.api.cache.CacheMetaCodec;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;

@ApiStatus.Internal
public class ClientRegisters {
    private ClientRegisters() {}
    public static final BiMap<ResourceLocation, CacheMetaCodec<? extends ITexSource, TexSourceDataHolder>> ITEXSOURCES = HashBiMap.create();
    public static final HashMap<Codec<? extends ITexSource>, CacheMetaCodec<? extends ITexSource, TexSourceDataHolder>> ITEXSOURCES_WRAPPED = new HashMap<>();
}
