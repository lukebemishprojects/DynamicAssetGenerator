/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.mixin;

import dev.lukebemish.dynamicassetgenerator.impl.ResourceFinder;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin {
    @ModifyVariable(method = "<init>", at = @At(value = "HEAD"), argsOnly = true)
    private static List<PackResources> dynamic_asset_generator$loadPacks(List<PackResources> packs, PackType type, List<PackResources> packsAgain) {
        ResourceFinder.INSTANCES[type.ordinal()] = packs::stream;
        return packs;
    }
}
