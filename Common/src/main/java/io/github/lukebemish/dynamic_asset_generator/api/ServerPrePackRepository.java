package io.github.lukebemish.dynamic_asset_generator.api;

import com.google.common.collect.ImmutableList;
import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.impl.platform.Services;
import io.github.lukebemish.dynamic_asset_generator.impl.util.InvisibleProviderUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ServerPrePackRepository {
    //Allows resources to be found while packs are being loaded... not sure how bad of an idea this is.
    private static List<PackResources> resources = new ArrayList<>();

    @ApiStatus.Internal
    public static void loadResources(List<PackResources> r) {
        for (var provider : InvisibleProviderUtils.INVISIBLE_RESOURCE_PROVIDERS)
            provider.reset(PackType.SERVER_DATA);
        resources = Stream.concat(
                r.stream()
                        .filter((p)->!(p.getName().contains(DynamicAssetGenerator.CLIENT_PACK) || p.getName().contains(DynamicAssetGenerator.SERVER_PACK))),
                InvisibleProviderUtils.INVISIBLE_RESOURCE_PROVIDERS.stream().map(InvisibleProviderUtils::constructPlaceholderResourcesFromProvider)
        ).collect(ImmutableList.toImmutableList());
    }

    @SuppressWarnings("unused")
    public static List<PackResources> getResources() {
        return resources;
    }

    @SuppressWarnings("unused")
    public static InputStream getResource(ResourceLocation rl) throws IOException {
        InputStream resource = null;
        for (PackResources r : resources) {
            if (r.hasResource(PackType.SERVER_DATA, rl)) {
                if (resource != null) resource.close();
                resource = r.getResource(PackType.SERVER_DATA, rl);
            }
        }
        if (resource != null) {
            return resource;
        }
        throw new IOException("Could not find data in pre-load: "+rl.toString());
    }

    @SuppressWarnings("unused")
    public static Stream<InputStream> getResources(ResourceLocation rl) throws IOException {
        List<InputStream> out = new ArrayList<>();
        for (PackResources r : Services.DEGROUPER.unpackPacks(resources)) {
            if (!r.getName().contains(DynamicAssetGenerator.SERVER_PACK) && r.hasResource(PackType.SERVER_DATA, rl)) {
                InputStream resource = r.getResource(PackType.SERVER_DATA, rl);
                if (resource!=null)
                    out.add(0, resource);
            }
        }
        if (!out.isEmpty()) {
            return out.stream();
        }
        throw new IOException("Could not find data in pre-load: "+rl.toString());
    }
}