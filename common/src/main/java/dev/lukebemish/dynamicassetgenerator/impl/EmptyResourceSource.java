/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl;

import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class EmptyResourceSource implements ResourceGenerationContext.ResourceSource {
    public static final EmptyResourceSource INSTANCE = new EmptyResourceSource();

    private EmptyResourceSource() {}

    @Override
    public @Nullable IoSupplier<InputStream> getResource(@NonNull ResourceLocation location) {
        return null;
    }

    @Override
    public List<IoSupplier<InputStream>> getResourceStack(@NonNull ResourceLocation location) {
        return List.of();
    }

    @Override
    public Map<ResourceLocation, IoSupplier<InputStream>> listResources(@NonNull String namespace, @NonNull Predicate<ResourceLocation> filter) {
        return Map.of();
    }

    @Override
    public Map<ResourceLocation, List<IoSupplier<InputStream>>> listResourceStacks(@NonNull String namespace, @NonNull Predicate<ResourceLocation> filter) {
        return Map.of();
    }

    @Override
    public @NonNull Set<String> getNamespaces() {
        return Set.of();
    }
}
