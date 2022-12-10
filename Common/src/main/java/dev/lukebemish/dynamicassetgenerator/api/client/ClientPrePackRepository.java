/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client;

import com.google.common.collect.ImmutableList;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.platform.Services;
import dev.lukebemish.dynamicassetgenerator.mixin.IPackRepositoryMixin;
import dev.lukebemish.dynamicassetgenerator.impl.util.InvisibleProviderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ClientPrePackRepository {
    //Allows resources to be found while packs are being loaded... not sure how bad of an idea this is.
    private static List<? extends PackResources> resources = new ArrayList<>();

    @ApiStatus.Internal
    public static void resetResources() {
        resources = null;
    }

    @SuppressWarnings("unused")
    public static List<? extends PackResources> getResources() {
        if (resources == null || resources.isEmpty()) {
            for (var provider : InvisibleProviderUtils.INVISIBLE_RESOURCE_PROVIDERS)
                provider.reset(PackType.CLIENT_RESOURCES);
            resources = Stream.concat(
                    Services.DEGROUPER.unpackPacks(((IPackRepositoryMixin) Minecraft.getInstance().getResourcePackRepository()).getSelected().stream()
                                    .filter(p->!(p.getId().startsWith(DynamicAssetGenerator.MOD_ID+':'))).map(Pack::open)
                                    .filter(p->!(p.packId().startsWith(DynamicAssetGenerator.MOD_ID+':')))
                                    .collect(ImmutableList.toImmutableList()))
                            .stream(),
                    InvisibleProviderUtils.INVISIBLE_RESOURCE_PROVIDERS.stream().map(InvisibleProviderUtils::constructPlaceholderResourcesFromProvider)
            ).collect(ImmutableList.toImmutableList());
        }
        return resources;
    }

    @SuppressWarnings("unused")
    public static InputStream getResource(ResourceLocation rl) throws IOException {
        IoSupplier<InputStream> resource = null;
        for (PackResources r : getResources()) {
            if (!r.packId().startsWith(DynamicAssetGenerator.MOD_ID+':')) {
                IoSupplier<InputStream> supplier = r.getResource(PackType.CLIENT_RESOURCES, rl);
                if (supplier == null) continue;
                resource = supplier;
            }
        }
        if (resource != null) {
            return resource.get();
        }
        throw new IOException("Could not find asset in pre-load: "+rl.toString());
    }

    @SuppressWarnings("unused")
    public static Stream<InputStream> getResources(ResourceLocation rl) throws IOException {
        List<InputStream> out = new ArrayList<>();
        for (PackResources r : getResources()) {
            if (!r.packId().startsWith(DynamicAssetGenerator.MOD_ID+':')) {
                IoSupplier<InputStream> supplier = r.getResource(PackType.CLIENT_RESOURCES, rl);
                if (supplier == null) continue;
                out.add(supplier.get());
            }
        }
        if (!out.isEmpty()) {
            return out.stream();
        }
        throw new IOException("Could not find asset in pre-load: "+rl.toString());
    }
}
