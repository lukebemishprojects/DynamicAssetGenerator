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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin {
    @Mutable
    @Shadow
    @Final
    private List<PackResources> packs;

    @Unique
    private PackType dynamic_asset_generator$type;

    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void dynamic_asset_generator$captureType(PackType type, List<PackResources> packs, CallbackInfo ci) {
        this.dynamic_asset_generator$type = type;
        this.dynamic_asset_generator$addPacksToTop();
    }

    private void dynamic_asset_generator$addPacksToTop() {
        if (!(this.packs instanceof ArrayList<PackResources>)) {
            this.packs = new ArrayList<>(this.packs);
        }
        for (Pack pack : PackPlanner.forType(dynamic_asset_generator$type).plan()) {
            if (pack.getDefaultPosition() == Pack.Position.TOP) {
                this.packs.add(pack.open());
            } else {
                this.packs.add(0, pack.open());
            }
        }
    }
}
