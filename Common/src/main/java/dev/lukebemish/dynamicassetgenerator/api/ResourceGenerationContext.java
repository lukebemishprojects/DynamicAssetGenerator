/*
 * Copyright (C) 2022 Luke Bemish and contributors
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

public abstract class ResourceGenerationContext {
    @Deprecated(forRemoval = true, since = "4.1.0")
    @NotNull public ResourceLocation cacheName() {
        return getCacheName();
    }

    @NotNull public abstract ResourceLocation getCacheName();

    @Nullable public abstract IoSupplier<InputStream> getResource(@NotNull ResourceLocation location);

    public abstract void listResources(@NotNull String namespace, @NotNull String path, @NotNull PackResources.ResourceOutput resourceOutput);

    @NotNull public abstract Set<String> getNamespaces();
}
