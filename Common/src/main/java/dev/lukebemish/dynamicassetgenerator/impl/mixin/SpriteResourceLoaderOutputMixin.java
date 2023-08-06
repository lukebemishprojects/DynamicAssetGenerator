/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.mixin;

import dev.lukebemish.dynamicassetgenerator.impl.client.ExposesName;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader$1")
public class SpriteResourceLoaderOutputMixin implements ExposesName {

    @Shadow(aliases = {"field_41390", "f_260614_"})
    @Final
    private SpriteResourceLoader this$0;

    @Override
    public ResourceLocation dynamicassetgenerator$getName() {
        return ((ExposesName) this$0).dynamicassetgenerator$getName();
    }
}
