package dynamic_asset_generator.api;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import dynamic_asset_generator.mixin.IPackRepositoryMixin;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PrePackRepository {
    //Allows resources to be found while packs are being loaded... not sure how bad of an idea this is.
    private static List<PackResources> resources;

    public static void resetResources() {
        resources = null;
    }

    private static List<PackResources> getResources() {
        if (resources == null) {
            resources = ((IPackRepositoryMixin)Minecraft.getInstance().getResourcePackRepository()).getSelected().stream().map(Pack::open).collect(ImmutableList.toImmutableList());
        }
        return resources;
    }

    public static InputStream getResource(ResourceLocation rl) throws IOException {
        InputStream resource = null;
        for (PackResources r : getResources()) {
            if (r.hasResource(PackType.CLIENT_RESOURCES, rl)) {
                resource = r.getResource(PackType.CLIENT_RESOURCES, rl);
            }
        }
        if (resource != null) {
            return resource;
        }
        throw new IOException("Could not find resource in pre-load: "+rl.toString());
    }
}
