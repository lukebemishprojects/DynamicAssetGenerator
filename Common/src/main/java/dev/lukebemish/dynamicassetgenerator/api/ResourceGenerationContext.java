/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Set;

/**
 * Information available during resource generation, passed to {@link InputStreamSource} as they are generated.
 */
public abstract class ResourceGenerationContext {
    /**
     * @deprecated Use {@link #getCacheName()} instead.
     */
    @Deprecated(forRemoval = true, since = "4.1.0")
    @NotNull public ResourceLocation cacheName() {
        return getCacheName();
    }

    /**
     * @return a resource location unique to the {@link ResourceCache} this context is linked to
     */
    @NotNull public abstract ResourceLocation getCacheName();

    /**
     * Attempts to get a resource at a given location, from the highest priority pack not provided by another {@link ResourceCache}.
     * @param location the location to get the resource at
     * @return a supplier for an input stream for the resource, or null if the resource does not exist
     */
    @Nullable public abstract IoSupplier<InputStream> getResource(@NotNull ResourceLocation location);

    /**
     * Lists all resources within a given path, from highest to lowest priority.
     * @param namespace the namespace to list resources from
     * @param path the path to list resources from
     * @param resourceOutput the output to write the resources to
     */
    @SuppressWarnings("unused")
    public abstract void listResources(@NotNull String namespace, @NotNull String path, @NotNull PackResources.ResourceOutput resourceOutput);

    /**
     * @return a set of all namespaces that have resources in this context
     */
    @SuppressWarnings("unused")
    @NotNull public abstract Set<String> getNamespaces();
}
