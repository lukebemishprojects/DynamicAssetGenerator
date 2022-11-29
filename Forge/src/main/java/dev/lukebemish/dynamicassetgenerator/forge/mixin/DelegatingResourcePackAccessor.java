/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.forge.mixin;

import net.minecraft.server.packs.PackResources;
import net.minecraftforge.resource.DelegatingPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = DelegatingPackResources.class, remap = false)
public interface DelegatingResourcePackAccessor {
    @Accessor
    List<PackResources> getDelegates();
}
