/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.mixin;

import dev.lukebemish.dynamicassetgenerator.impl.ResourceFinder;
import dev.lukebemish.dynamicassetgenerator.impl.platform.Services;
import dev.lukebemish.dynamicassetgenerator.impl.util.InvisibleProviderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(method = {"reloadResourcePacks(Z)Ljava/util/concurrent/CompletableFuture;", "<init>"}, ordinal = 0, require = 0,
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/server/packs/repository/PackRepository;openAllSelected()Ljava/util/List;",shift = At.Shift.AFTER))
    private List<PackResources> dynamic_asset_generator$modifyList(List<PackResources> resources) {
        ResourceFinder.INSTANCES[PackType.CLIENT_RESOURCES.ordinal()] = () -> {
            ArrayList<PackResources> out = new ArrayList<>(Services.DEGROUPER.unpackPacks(((PackRepositoryMixin) Minecraft.getInstance()
                .getResourcePackRepository()).getSelected().stream().map(Pack::open).toList()));
            InvisibleProviderUtils.INVISIBLE_RESOURCE_PROVIDERS.stream().map(InvisibleProviderUtils::constructPlaceholderResourcesFromProvider).forEach(out::add);
            return out;
        };
        return resources;
    }
}
