/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.mixin;

import dev.lukebemish.dynamicassetgenerator.impl.client.ExposesName;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteResourceLoader.class)
public class SpriteResourceLoaderMixin implements ExposesName {
    @Unique
    private ResourceLocation name;

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "load", at = @At("RETURN"))
    private static void dynamicassetgenerator$load(ResourceManager pResourceManager, ResourceLocation pLocation, CallbackInfoReturnable<SpriteResourceLoader> cir) {
        ((SpriteResourceLoaderMixin) (Object) cir.getReturnValue()).name = pLocation;
    }

    @Override
    public ResourceLocation dynamicassetgenerator$getName() {
        return name;
    }
}
