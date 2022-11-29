/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import dev.lukebemish.dynamicassetgenerator.api.IResourceGenerator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class CommonRegisters {
    private CommonRegisters() {}
    public static final BiMap<ResourceLocation, Codec<? extends IResourceGenerator>> IRESOURCEGENERATORS = HashBiMap.create();
}
