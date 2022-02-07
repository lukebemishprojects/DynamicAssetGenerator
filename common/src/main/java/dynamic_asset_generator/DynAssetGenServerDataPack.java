package dynamic_asset_generator;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DynAssetGenServerDataPack implements PackResources {

    private final Map<ResourceLocation, Supplier<InputStream>> istreams;

    public DynAssetGenServerDataPack() {
        istreams = DynAssetGenServerPlanner.getResources();
    }

    @Nullable
    @Override
    public InputStream getRootResource(String location) throws IOException {
        throw new IOException("Could not find resource in generated data: " + location.toString());
    }

    @Override
    public InputStream getResource(PackType packType, ResourceLocation location) throws IOException {
        if (packType == PackType.SERVER_DATA) {
            if (istreams.containsKey(location)) {
                InputStream stream = istreams.get(location).get();
                if (stream != null) {
                    return stream;
                } else {
                    throw new IOException("Data is null: " + location.toString());
                }
            }
        }
        throw new IOException("Could not find resource in generated data: " + location.toString());
    }

    @Override
    public Collection<ResourceLocation> getResources(PackType packType, String namespace, String directory, int depth, Predicate<String> predicate) {
        ArrayList<ResourceLocation> locations = new ArrayList<>();
        if (packType == PackType.SERVER_DATA) {
            for (ResourceLocation key : istreams.keySet()) {
                if (key.toString().startsWith(directory) && key.getNamespace().equals(namespace) && predicate.test(key.getPath()) && istreams.get(key).get() != null) {
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
            if (istreams.containsKey(location)) {
                return istreams.get(location).get() != null;
            }
        }
        return false;
    }

    @Override
    public Set<String> getNamespaces(PackType packType) {
        Set<String> namespaces = new HashSet<>();
        if (packType == PackType.SERVER_DATA) {
            for (ResourceLocation key : istreams.keySet()) {
                namespaces.add(key.getNamespace());
            }
        }
        return namespaces;
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
        return null;
    }

    @Override
    public String getName() {
        return DynamicAssetGenerator.MOD_ID+"_generated";
    }

    @Override
    public void close() {

    }
}
