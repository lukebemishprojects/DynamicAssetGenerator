/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class CommonRegisters {
    private CommonRegisters() {}
    public static final BiMap<ResourceLocation, Codec<? extends ResourceGenerator>> RESOURCEGENERATORS = HashBiMap.create();
}
