/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.fabric;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.FabriQuiltShared;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.RepositorySource;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FabricPlatform implements FabriQuiltShared {
    public static final FabriQuiltShared INSTANCE = new FabricPlatform();

    private static final String GROUP_PACK_CLASS = "net.fabricmc.fabric.impl.resource.loader.GroupResourcePack";
    private static final Class<?> GROUP_PACK_RESOURCES;
    private static final MethodHandle GET_GROUP_PACK_PACKS;

    static {
        Class<?> clazz;
        try {
            clazz = FabricPlatform.class.getClassLoader().loadClass(GROUP_PACK_CLASS);
        } catch (ClassNotFoundException e) {
            clazz = null;
        }
        if (clazz == null) {
            GROUP_PACK_RESOURCES = null;
            GET_GROUP_PACK_PACKS = null;
        } else {
            GROUP_PACK_RESOURCES = clazz;
            var lookup = MethodHandles.lookup();
            MethodHandle getter;
            try {
                var privateLookup = MethodHandles.privateLookupIn(clazz, lookup);
                @SuppressWarnings("rawtypes") Class<List> listClazz = List.class;
                getter = privateLookup.findGetter(clazz, "packs", listClazz);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                getter = null;
            }
            GET_GROUP_PACK_PACKS = getter;
        }
        if (GROUP_PACK_RESOURCES == null || GET_GROUP_PACK_PACKS == null) {
            DynamicAssetGenerator.LOGGER.error("On fabric (not quilt) but could not find fabric internal class/field to unwrap grouped resources - Dynamic Asset Generator may not work right!");
        }
    }

    @Override
    public void packForType(PackType type, RepositorySource source) {
        PackPlanner.forType(type).register(source);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    @SuppressWarnings("deprecation")
    @Override
    public String modVersion(String id) {
        return FabricLoader.getInstance().getModContainer(id).orElseThrow().getMetadata().getVersion().getFriendlyString();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Path cacheDir() {
        return FabricLoader.getInstance().getGameDir().resolve(".cache");
    }

    private static final boolean[] LOGGED_ERROR = new boolean[1];

    private synchronized void logError(int i) {
        if (!LOGGED_ERROR[i]) {
            if (i == 0) {
                DynamicAssetGenerator.LOGGER.error("On fabric (not quilt) but could not properly use fabric internal class/field to unwrap grouped resources - Dynamic Asset Generator may not work right!");
            }
            LOGGED_ERROR[i] = true;
        }
    }

    @Override
    public List<? extends PackResources> unpackPacks(List<? extends PackResources> packs) {
        ArrayList<PackResources> packsOut = new ArrayList<>();
        packs.forEach(pack -> {
            if (GROUP_PACK_RESOURCES != null && GET_GROUP_PACK_PACKS != null) {
                if (GROUP_PACK_RESOURCES.isInstance(pack)) {
                    try {
                        @SuppressWarnings({"unchecked", "rawtypes"}) List<? extends PackResources> unpackedPacks =
                            (List<? extends PackResources>) (List) GET_GROUP_PACK_PACKS.invoke(pack);
                        packsOut.addAll(unpackedPacks);
                    } catch (Throwable e) {
                        logError(0);
                    }
                }
            }
        });
        return packsOut;
    }
}
