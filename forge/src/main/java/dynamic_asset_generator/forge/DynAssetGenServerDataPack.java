package dynamic_asset_generator.forge;

import com.google.gson.JsonObject;
import dynamic_asset_generator.DynAssetGenServerPlanner;
import dynamic_asset_generator.DynamicAssetGenerator;
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

    private Map<ResourceLocation, Supplier<InputStream>> streams;

    private static final int PACK_VERSION = 8;

    private Map<ResourceLocation, Supplier<InputStream>> getStreams() {
        if (streams == null) {
            streams = DynAssetGenServerPlanner.getResources();
        }
        return streams;
    }

    public DynAssetGenServerDataPack() {
    }

    @Nullable
    @Override
    public InputStream getRootResource(String location) throws IOException {
        if(!location.contains("/") && !location.contains("\\")) {
            Supplier<InputStream> supplier = this.getStreams().get(location);
            return supplier.get();
        } else {
            throw new IllegalArgumentException("File name can't be a path");
        }
    }

    @Override
    public InputStream getResource(PackType packType, ResourceLocation location) throws IOException {
        if (packType == PackType.SERVER_DATA) {
            if (getStreams().containsKey(location)) {
                InputStream stream = getStreams().get(location).get();
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
            for (ResourceLocation key : getStreams().keySet()) {
                if (key.toString().startsWith(directory) && key.getNamespace().equals(namespace) && predicate.test(key.getPath()) && getStreams().get(key).get() != null) {
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
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
        if(serializer.getMetadataSectionName().equals("pack")) {
            JsonObject object = new JsonObject();
            object.addProperty("pack_format", PACK_VERSION);
            object.addProperty("description", "dynamically generated assets");
            return serializer.fromJson(object);
        }
        DynamicAssetGenerator.LOGGER.info("'" + serializer.getMetadataSectionName() + "' is an unsupported metadata key!");
        return serializer.fromJson(new JsonObject());
    }

    @Override
    public String getName() {
        return DynamicAssetGenerator.SERVER_PACK;
    }

    @Override
    public void close() {

    }
}
