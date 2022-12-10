/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl;

import dev.lukebemish.dynamicassetgenerator.api.DataResourceCache;
import dev.lukebemish.dynamicassetgenerator.api.ServerPrePackRepository;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BuiltinDataResourceCache extends DataResourceCache {
    public BuiltinDataResourceCache(ResourceLocation name) {
        super(name);
        this.planSource(() -> new JsonResourceGeneratorReader(getSourceJsons()));
    }

    static Map<ResourceLocation, String> getSourceJsons() {
        HashMap<ResourceLocation, String> rls = new HashMap<>();
        Map<ResourceLocation, IoSupplier<InputStream>> available = new HashMap<>();

        for (PackResources r : ServerPrePackRepository.getResources()) {
            if (r.packId().startsWith(DynamicAssetGenerator.MOD_ID+':')) continue;
            for (String namespace : r.getNamespaces(PackType.SERVER_DATA)) {
                r.listResources(PackType.SERVER_DATA, namespace, SOURCE_JSON_DIR,  (rl, streamSupplier) -> {
                    if (rl.getPath().endsWith(".json") && streamSupplier != null) {
                        available.put(rl, streamSupplier);
                    }
                });
            }
        }

        available.forEach((rl, streamSupplier) -> {
            try (InputStream stream = streamSupplier.get()) {
                byte[] bytes = stream.readAllBytes();
                String json = new String(bytes, StandardCharsets.UTF_8);
                rls.put(rl, json);
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Error reading source json: " + rl, e);
            }
        });

        return rls;
    }
}
