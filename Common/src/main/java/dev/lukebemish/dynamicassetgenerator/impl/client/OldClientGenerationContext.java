/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client;

import dev.lukebemish.dynamicassetgenerator.api.client.ClientPrePackRepository;
import dev.lukebemish.dynamicassetgenerator.impl.OldResourceGenerationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class OldClientGenerationContext extends OldResourceGenerationContext {
    public OldClientGenerationContext(ResourceLocation cacheName) {
        super(cacheName);
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(@NotNull ResourceLocation location) {
        var packs = ClientPrePackRepository.getResources();

        for (var pack : packs) {
            var resource = pack.getResource(PackType.CLIENT_RESOURCES, location);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    @Override
    public void listResources(@NotNull String namespace, @NotNull String path, PackResources.@NotNull ResourceOutput resourceOutput) {
        var packs = ClientPrePackRepository.getResources();

        for (var pack : packs) {
            pack.listResources(PackType.CLIENT_RESOURCES, namespace, path, resourceOutput);
        }
    }

    @Override
    public @NotNull Set<String> getNamespaces() {
        var namespaces = new HashSet<String>();
        var packs = ClientPrePackRepository.getResources();

        for (var pack : packs) {
            namespaces.addAll(pack.getNamespaces(PackType.CLIENT_RESOURCES));
        }

        return namespaces;
    }
}
