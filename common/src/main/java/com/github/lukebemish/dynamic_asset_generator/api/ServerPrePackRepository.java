package com.github.lukebemish.dynamic_asset_generator.api;

import com.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import com.google.common.collect.ImmutableList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ServerPrePackRepository {
    //Allows resources to be found while packs are being loaded... not sure how bad of an idea this is.
    private static List<PackResources> resources = new ArrayList<>();

    public static void loadResources(List<PackResources> r) {
        resources = r.stream()
                .filter((p)->!(p.getName().contains(DynamicAssetGenerator.CLIENT_PACK) || p.getName().contains(DynamicAssetGenerator.SERVER_PACK))).collect(ImmutableList.toImmutableList());
    }

    public static InputStream getResource(ResourceLocation rl) throws IOException {
        InputStream resource = null;
        for (PackResources r : resources) {
            if (!r.getName().equals(DynamicAssetGenerator.SERVER_PACK) && r.hasResource(PackType.SERVER_DATA, rl)) {
                resource = r.getResource(PackType.SERVER_DATA, rl);
            }
        }
        if (resource != null) {
            return resource;
        }
        throw new IOException("Could not find data in pre-load: "+rl.toString());
    }

    public static List<InputStream> getResources(ResourceLocation rl) throws IOException {
        List<InputStream> out = new ArrayList<>();
        for (PackResources r : resources) {
            if (!r.getName().equals(DynamicAssetGenerator.SERVER_PACK) && r.hasResource(PackType.SERVER_DATA, rl)) {
                out.add(0, r.getResource(PackType.SERVER_DATA, rl));
            }
        }
        if (out.size() != 0) {
            return out;
        }
        throw new IOException("Could not find data in pre-load: "+rl.toString());
    }
}