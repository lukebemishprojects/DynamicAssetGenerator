package io.github.lukebemish.dynamic_asset_generator.impl;

import io.github.lukebemish.dynamic_asset_generator.api.DataResourceCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class DynAssetGenServerDataPack implements PackResources {

    private Map<ResourceLocation, Supplier<InputStream>> streams;

    private Map<ResourceLocation, Supplier<InputStream>> getStreams() {
        if (streams == null) {
            streams = DataResourceCache.INSTANCE.getResources();
        }
        return streams;
    }

    public DynAssetGenServerDataPack() {
        DataResourceCache.INSTANCE.reset();
    }

    @Nullable
    @Override
    public InputStream getRootResource(String location) {
        return null;
    }

    @Override
    public InputStream getResource(PackType packType, ResourceLocation location) throws IOException {
        if (packType == PackType.SERVER_DATA) {
            if (getStreams().containsKey(location)) {
                InputStream stream = getStreams().get(location).get();
                if (stream != null) {
                    return stream;
                } else {
                    throw new IOException("Data is null: " + location);
                }
            }
        }
        throw new IOException("Could not find resource in generated data: " + location);
    }

    @Override
    public Collection<ResourceLocation> getResources(PackType packType, String namespace, String directory, Predicate<ResourceLocation> predicate) {
        ArrayList<ResourceLocation> locations = new ArrayList<>();
        if (packType == PackType.SERVER_DATA) {
            for (ResourceLocation key : getStreams().keySet()) {
                if (key.getPath().startsWith(directory) && key.getNamespace().equals(namespace) && predicate.test(key) && getStreams().get(key).get() != null) {
                    // still need to figure out depth...
                    locations.add(key);
                }
            }
        }
        return locations;
    }

    @Override
    public boolean hasResource(PackType packType, ResourceLocation location) {
        if (packType == PackType.SERVER_DATA) {
            if (getStreams().containsKey(location)) {
                return getStreams().get(location).get() != null;
            }
        }
        return false;
    }

    @Override
    public Set<String> getNamespaces(PackType packType) {
        Set<String> namespaces = new HashSet<>();
        if (packType == PackType.SERVER_DATA) {
            for (ResourceLocation key : getStreams().keySet()) {
                namespaces.add(key.getNamespace());
            }
        }
        return namespaces;
    }


    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        if (serializer.getMetadataSectionName().equals("pack")) {
            return (T) DynamicAssetGenerator.SERVER_PACK_METADATA;
        }
        return null;
    }

    @Override
    public String getName() {
        return DynamicAssetGenerator.SERVER_PACK;
    }

    @Override
    public void close() {

    }
}
