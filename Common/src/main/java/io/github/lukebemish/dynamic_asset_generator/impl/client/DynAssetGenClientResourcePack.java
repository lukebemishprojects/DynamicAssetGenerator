package io.github.lukebemish.dynamic_asset_generator.impl.client;

import io.github.lukebemish.dynamic_asset_generator.api.client.AssetResourceCache;
import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class DynAssetGenClientResourcePack implements PackResources {

    private Map<ResourceLocation, Supplier<InputStream>> streams;

    private Map<ResourceLocation, Supplier<InputStream>> getStreams() {
        if (streams == null) {
            streams = AssetResourceCache.INSTANCE.getResources();
        }
        return streams;
    }

    public DynAssetGenClientResourcePack() {
        AssetResourceCache.INSTANCE.reset();
    }

    @Nullable
    @Override
    public InputStream getRootResource(@NotNull String location) {
        return null;
    }

    @Override
    public @NotNull InputStream getResource(@NotNull PackType packType, @NotNull ResourceLocation location) throws IOException {
        if (packType == PackType.CLIENT_RESOURCES) {
            if (getStreams().containsKey(location)) {
                InputStream stream = getStreams().get(location).get();
                if (stream != null) {
                    return stream;
                } else {
                    throw new IOException("Resource is null: " + location);
                }
            }
        }
        throw new IOException("Could not find resource in generated resources: " + location);
    }

    @Override
    public @NotNull Collection<ResourceLocation> getResources(@NotNull PackType packType, @NotNull String namespace, @NotNull String directory, @NotNull Predicate<ResourceLocation> predicate) {
        ArrayList<ResourceLocation> locations = new ArrayList<>();
        if (packType == PackType.CLIENT_RESOURCES) {
            for (ResourceLocation key : getStreams().keySet()) {
                if (key.getPath().startsWith(directory) && key.getNamespace().equals(namespace) && predicate.test(key)) {
                    // still need to figure out depth...
                    locations.add(key);
                }
            }
        }
        return locations;
    }

    @Override
    public boolean hasResource(@NotNull PackType packType, @NotNull ResourceLocation location) {
        if (packType == PackType.CLIENT_RESOURCES) {
            if (getStreams().containsKey(location)) {
                return getStreams().get(location).get() != null;
            }
        }
        return false;
    }

    @Override
    public @NotNull Set<String> getNamespaces(@NotNull PackType packType) {
        Set<String> namespaces = new HashSet<>();
        if (packType == PackType.CLIENT_RESOURCES) {
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
            return (T) DynamicAssetGenerator.CLIENT_PACK_METADATA;
        }
        return null;
    }

    @Override
    public @NotNull String getName() {
        return DynamicAssetGenerator.CLIENT_PACK;
    }

    @Override
    public void close() {

    }
}
