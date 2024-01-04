/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl;

import dev.lukebemish.dynamicassetgenerator.api.ServerPrePackRepository;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class OldServerGenerationContext extends OldResourceGenerationContext {
    public OldServerGenerationContext(ResourceLocation cacheName) {
        super(cacheName);
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(@NotNull ResourceLocation location) {
        var packs = ServerPrePackRepository.getResources();

        IoSupplier<InputStream> found = null;
        for (var pack : packs) {
            var resource = pack.getResource(PackType.SERVER_DATA, location);
            if (resource != null) {
                found = resource;
            }
        }
        return found;
    }

    @Override
    public void listResources(@NotNull String namespace, @NotNull String path, PackResources.@NotNull ResourceOutput resourceOutput) {
        var packs = ServerPrePackRepository.getResources();

        for (var pack : packs) {
            pack.listResources(PackType.SERVER_DATA, namespace, path, resourceOutput);
        }
    }

    @Override
    public @NotNull Set<String> getNamespaces() {
        var namespaces = new HashSet<String>();
        var packs = ServerPrePackRepository.getResources();

        for (var pack : packs) {
            namespaces.addAll(pack.getNamespaces(PackType.SERVER_DATA));
        }

        return namespaces;
    }
}
