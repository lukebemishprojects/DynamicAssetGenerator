/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.quilt;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.FabriQuiltShared;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.RepositorySource;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.resource.loader.api.GroupResourcePack;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class QuiltPlatform implements FabriQuiltShared {
    public static final FabriQuiltShared INSTANCE = new QuiltPlatform();

    @Override
    public void packForType(PackType type, RepositorySource source) {
        // Can't use the "default" and "top" events as they compute namespaces too early
        ResourceLoader.get(type).registerResourcePackProfileProvider(source);
    }

    @Override
    public boolean isModLoaded(String id) {
        return QuiltLoader.isModLoaded(id);
    }

    @Override
    public String modVersion(String id) {
        return QuiltLoader.getModContainer(DynamicAssetGenerator.MOD_ID).orElseThrow().metadata().version().raw();
    }

    @Override
    public Path configDir() {
        return QuiltLoader.getConfigDir();
    }

    @Override
    public Path cacheDir() {
        return QuiltLoader.getCacheDir();
    }

    @Override
    public List<? extends PackResources> unpackPacks(List<? extends PackResources> packs) {
        ArrayList<PackResources> packsOut = new ArrayList<>();
        packs.forEach(pack -> {
            if (pack instanceof GroupResourcePack groupResourcePack) {
                packsOut.addAll(groupResourcePack.getPacks());
            } else packsOut.add(pack);
        });
        return packsOut;
    }
}
