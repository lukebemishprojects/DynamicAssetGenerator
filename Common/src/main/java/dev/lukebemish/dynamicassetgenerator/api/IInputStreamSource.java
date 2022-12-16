/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

@FunctionalInterface
public interface IInputStreamSource {
    /**
     * Gets an input stream for the given resource location.
     *
     * @param outRl   {@link ResourceLocation} to get the input stream for.
     * @param context {@link ResourceGenerationContext} containing information about when and where the resource is being generated.
     * @return Supplier for an InputStream for the location. Should be null if the resource cannot be loaded.
     */
    @Nullable
    IoSupplier<InputStream> get(ResourceLocation outRl, ResourceGenerationContext context);
}
