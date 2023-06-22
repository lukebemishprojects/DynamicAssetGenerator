/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import com.google.common.collect.ImmutableList;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.util.InvisibleProviderUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class should be considered internal, but to avoid breaking backwards compatibility, no breaking changes will be
 * made until DynAssetGen 5.0.0 or later.
 */
@ApiStatus.Internal
public class ServerPrePackRepository {
    //Allows resources to be found while packs are being loaded... not sure how bad of an idea this is.
    private static List<PackResources> resources = new ArrayList<>();

    @ApiStatus.Internal
    public static void loadResources(List<PackResources> r) {
        for (var provider : InvisibleProviderUtils.INVISIBLE_RESOURCE_PROVIDERS)
            provider.reset(PackType.SERVER_DATA);
        resources = Stream.concat(
                r.stream()
                        .filter((p)->!p.packId().startsWith(DynamicAssetGenerator.MOD_ID+':')),
                InvisibleProviderUtils.INVISIBLE_RESOURCE_PROVIDERS.stream().map(InvisibleProviderUtils::constructPlaceholderResourcesFromProvider)
        ).collect(ImmutableList.toImmutableList());
    }

    @SuppressWarnings("unused")
    public static List<PackResources> getResources() {
        return resources;
    }

    @SuppressWarnings("unused")
    public static InputStream getResource(ResourceLocation rl) throws IOException {
        IoSupplier<InputStream> resource = null;
        for (PackResources r : getResources()) {
            if (!r.packId().startsWith(DynamicAssetGenerator.MOD_ID+':')) {
                IoSupplier<InputStream> supplier = r.getResource(PackType.SERVER_DATA, rl);
                if (supplier == null) continue;
                resource = supplier;
            }
        }
        if (resource != null) {
            return resource.get();
        }
        throw new IOException("Could not find data in pre-load: "+rl.toString());
    }

    @SuppressWarnings("unused")
    public static Stream<InputStream> getResources(ResourceLocation rl) throws IOException {
        List<InputStream> out = new ArrayList<>();
        for (PackResources r : getResources()) {
            if (!r.packId().startsWith(DynamicAssetGenerator.MOD_ID+':')) {
                IoSupplier<InputStream> supplier = r.getResource(PackType.SERVER_DATA, rl);
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
