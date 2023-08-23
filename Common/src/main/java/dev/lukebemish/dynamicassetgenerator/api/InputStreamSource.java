/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * An object that can provide an input stream supplier at a given location.
 */
@FunctionalInterface
public interface InputStreamSource {
    /**
     * Gets an input stream for the given resource location.
     *
     * @param outRl   {@link ResourceLocation} to get the input stream for.
     * @param context {@link ResourceGenerationContext} containing information about when and where the resource is being generated.
     * @return Supplier for an InputStream for the location. Should be null if the resource cannot be loaded.
     */
    @Nullable
    IoSupplier<InputStream> get(ResourceLocation outRl, ResourceGenerationContext context);

    /**
     * Create a key that can be <em>uniquely</em> used to identify the resource this generator will generate. Note that
     * this is used for caching across reloads, and so should incorporate any resources that may be used to generate the
     * resource. If this is not possible, return null.
     * @param outRl the resource location that will be generated
     * @param context the context that the resource will be generated in. Resources can safely be accessed in this context
     * @return a key that can be used to uniquely identify the resource, or null if this is not possible
     */
    @ApiStatus.Experimental
    default @Nullable String createCacheKey(ResourceLocation outRl, ResourceGenerationContext context) {
        return null;
    }
}
