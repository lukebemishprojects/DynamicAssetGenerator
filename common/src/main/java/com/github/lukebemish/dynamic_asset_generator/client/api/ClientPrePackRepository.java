package com.github.lukebemish.dynamic_asset_generator.client.api;

import com.google.common.collect.ImmutableList;
import com.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import com.github.lukebemish.dynamic_asset_generator.mixin.IPackRepositoryMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ClientPrePackRepository {
    //Allows resources to be found while packs are being loaded... not sure how bad of an idea this is.
    private static List<PackResources> resources = new ArrayList<>();

    public static final String SOURCE_JSON_DIR = "dynamic_assets_sources";

    public static void resetResources() {
        resources = null;
    }

    private static List<PackResources> getResources() {
        if (resources == null || resources.size() == 0) {
            resources = ((IPackRepositoryMixin) Minecraft.getInstance().getResourcePackRepository()).getSelected().stream()
                    .filter((p)->!(p.getId().contains(DynamicAssetGenerator.CLIENT_PACK) || p.getId().contains(DynamicAssetGenerator.SERVER_PACK))).map(Pack::open)
                    .filter((p)->!(p.getName().contains(DynamicAssetGenerator.CLIENT_PACK) || p.getName().contains(DynamicAssetGenerator.SERVER_PACK))).collect(ImmutableList.toImmutableList());
        }
        return resources;
    }

    public static InputStream getResource(ResourceLocation rl) throws IOException {
        InputStream resource = null;
        for (PackResources r : getResources()) {
            if (!r.getName().equals(DynamicAssetGenerator.CLIENT_PACK) && r.hasResource(PackType.CLIENT_RESOURCES, rl)) {
                resource = r.getResource(PackType.CLIENT_RESOURCES, rl);
            }
        }
        if (resource != null) {
            return resource;
        }
        throw new IOException("Could not find resource in pre-load: "+rl.toString());
    }

    public static HashMap<ResourceLocation, String> getSourceJsons() {
        HashMap<ResourceLocation, String> rls = new HashMap<>();
        HashSet<ResourceLocation> available = new HashSet<>();

        for (PackResources r : getResources()) {
            if (r.getName().equals(DynamicAssetGenerator.CLIENT_PACK) || r.getName().equals(DynamicAssetGenerator.SERVER_PACK)) continue;
            for (String namespace : r.getNamespaces(PackType.CLIENT_RESOURCES)) {
                for (ResourceLocation rl : r.getResources(PackType.CLIENT_RESOURCES, namespace, SOURCE_JSON_DIR, 6, (x)->x.endsWith(".json"))) {
                    if (r.hasResource(PackType.CLIENT_RESOURCES,rl)) available.add(rl);
                }
            }
        }

        for (ResourceLocation rl : available) {
            try {
                InputStream resource = getResource(rl);
                String text = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
                rls.put(rl, text);
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Issues loading texture source jsons...");
            }
        }
        return rls;
    }
}
