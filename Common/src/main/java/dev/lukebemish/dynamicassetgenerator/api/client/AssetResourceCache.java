/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client;

import dev.lukebemish.dynamicassetgenerator.api.ResourceCache;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.ModConfig;
import dev.lukebemish.dynamicassetgenerator.impl.client.JsonResourceGeneratorReader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class AssetResourceCache extends ResourceCache {
    public static final AssetResourceCache INSTANCE = new AssetResourceCache();
    @SuppressWarnings("unused")
    public static final ResourceLocation EMPTY_TEXTURE = new ResourceLocation(DynamicAssetGenerator.MOD_ID, "textures/empty.png");

    private AssetResourceCache() {
        planSource(() -> new JsonResourceGeneratorReader(getSourceJsons()));
    }

    static Map<ResourceLocation, String> getSourceJsons() {
        HashMap<ResourceLocation, String> rls = new HashMap<>();
        HashSet<ResourceLocation> available = new HashSet<>();

        for (PackResources r : ClientPrePackRepository.getResources()) {
            if (r.getName().equals(DynamicAssetGenerator.CLIENT_PACK) || r.getName().equals(DynamicAssetGenerator.SERVER_PACK)) continue;
            for (String namespace : r.getNamespaces(PackType.CLIENT_RESOURCES)) {
                for (ResourceLocation rl : r.getResources(PackType.CLIENT_RESOURCES, namespace, SOURCE_JSON_DIR, x->x.toString().endsWith(".json"))) {
                    if (r.hasResource(PackType.CLIENT_RESOURCES,rl)) available.add(rl);
                }
            }
        }

        for (ResourceLocation rl : available) {
            try (InputStream resource = ClientPrePackRepository.getResource(rl)) {
                String text = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
                rls.put(rl, text);
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Issues loading resource source jsons...");
            }
        }
        return rls;
    }

    @Override
    public boolean shouldCache() {
        return DynamicAssetGenerator.getConfig().cacheAssets();
    }

    @Override
    public Path cachePath() {
        return ModConfig.ASSET_CACHE_FOLDER;
    }
}
