/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.fabric.mixin;

import dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.fabric.PackPlanner;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin {

    @Unique
    private PackType dynamic_asset_generator$type;

    @ModifyVariable(
        method = "<init>",
        at = @At("HEAD"),
        argsOnly = true
    )
    private List<PackResources> dynamic_asset_generator$captureType(List<PackResources> packs, PackType type, List<PackResources> packs2) {
        this.dynamic_asset_generator$type = type;
        return this.dynamic_asset_generator$addPacksToTop(packs);
    }

    private List<PackResources> dynamic_asset_generator$addPacksToTop(List<PackResources> packs) {
        packs = new ArrayList<>(packs);
        for (Pack pack : PackPlanner.forType(dynamic_asset_generator$type).plan()) {
            if (pack.getDefaultPosition() == Pack.Position.TOP) {
                packs.add(pack.open());
            } else {
                packs.add(0, pack.open());
            }
        }
        return packs;
    }
}
