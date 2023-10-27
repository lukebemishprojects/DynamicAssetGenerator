/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.fabric.mixin;

import dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.fabric.PackPlanner;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.ServerPacksSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(BuiltInPackSource.class)
public class BuiltInPackSourceMixin {
    @Inject(method = "listBundledPacks", at = @At("RETURN"))
    private void addAdditionalPacks(Consumer<Pack> profileAdder, CallbackInfo ci) {
        //noinspection ConstantValue
        if (((Object) this) instanceof ServerPacksSource) {
            for (Pack pack : PackPlanner.forType(PackType.SERVER_DATA).plan()) {
                profileAdder.accept(pack);
            }
        }
    }
}
