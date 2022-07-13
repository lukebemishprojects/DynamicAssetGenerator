package io.github.lukebemish.dynamic_asset_generator.api.client;

import com.google.common.collect.ImmutableList;
import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.mixin.IPackRepositoryMixin;
import io.github.lukebemish.dynamic_asset_generator.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ClientPrePackRepository {
    //Allows resources to be found while packs are being loaded... not sure how bad of an idea this is.
    private static List<? extends PackResources> resources = new ArrayList<>();

    public static void resetResources() {
        resources = null;
    }

    public static List<? extends PackResources> getResources() {
        if (resources == null || resources.isEmpty()) {
            resources = Services.DEGROUPER.unpackPacks(((IPackRepositoryMixin) Minecraft.getInstance().getResourcePackRepository()).getSelected().stream()
                    .filter(p->!(p.getId().contains(DynamicAssetGenerator.CLIENT_PACK) || p.getId().contains(DynamicAssetGenerator.SERVER_PACK))).map(Pack::open)
                    .filter(p->!(p.getName().contains(DynamicAssetGenerator.CLIENT_PACK) || p.getName().contains(DynamicAssetGenerator.SERVER_PACK))).collect(ImmutableList.toImmutableList()));
        }
        return resources;
    }

    public static InputStream getResource(ResourceLocation rl) throws IOException {
        InputStream resource = null;
        for (PackResources r : getResources()) {
            if (!r.getName().equals(DynamicAssetGenerator.CLIENT_PACK) && r.hasResource(PackType.CLIENT_RESOURCES, rl)) {
                if (resource!=null) resource.close();
                resource = r.getResource(PackType.CLIENT_RESOURCES, rl);
            }
        }
        if (resource != null) {
            return resource;
        }
        throw new IOException("Could not find resource in pre-load: "+rl.toString());
    }

    public static List<InputStream> getResources(ResourceLocation rl) throws IOException {
        List<InputStream> out = new ArrayList<>();
        for (PackResources r : getResources()) {
            if (!r.getName().equals(DynamicAssetGenerator.SERVER_PACK) && r.hasResource(PackType.SERVER_DATA, rl)) {
                InputStream resource = r.getResource(PackType.SERVER_DATA, rl);
                if (resource!=null)
                    out.add(0, resource);
            }
        }
        if (!out.isEmpty()) {
            return out;
        }
        throw new IOException("Could not find asset in pre-load: "+rl.toString());
    }
}
