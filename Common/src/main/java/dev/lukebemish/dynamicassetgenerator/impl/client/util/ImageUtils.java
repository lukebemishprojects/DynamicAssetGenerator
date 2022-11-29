/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client.util;

import com.mojang.blaze3d.platform.NativeImage;
import dev.lukebemish.dynamicassetgenerator.api.client.ClientPrePackRepository;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

public class ImageUtils {
    public static NativeImage getImage(ResourceLocation location) throws IOException {
        return NativeImage.read(ClientPrePackRepository.getResource(location));
    }
}
