package palette_extractor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;

public class PaletteExtractorClientResourcePack implements PackResources {

    private final Map<ResourceLocation, BufferedImage> images;

    public PaletteExtractorClientResourcePack() {
        PrePackRepository.resetResources();
        images = PaletteExtractorPlanner.getImages();
    }

    @Nullable
    @Override
    public InputStream getRootResource(String location) throws IOException {
        throw new IOException("Could not find resource in generated resources: " + location.toString());
    }

    @Override
    public InputStream getResource(PackType packType, ResourceLocation location) throws IOException {
        if (packType == PackType.CLIENT_RESOURCES) {
            if (images.containsKey(location)) {
                BufferedImage image = images.get(location);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(image, "png", os);
                InputStream is = new ByteArrayInputStream(os.toByteArray());
                return is;
            }
        }
        throw new IOException("Could not find resource in generated resources: " + location.toString());
    }

    @Override
    public Collection<ResourceLocation> getResources(PackType packType, String namespace, String directory, int depth, Predicate<String> predicate) {
        ArrayList<ResourceLocation> locations = new ArrayList<>();
        if (packType == PackType.CLIENT_RESOURCES) {
            for (ResourceLocation key : images.keySet()) {
                if (key.toString().startsWith(directory) && key.getNamespace().equals(namespace) && predicate.test(key.getPath())) {
                    // still need to figure out depth...
                    locations.add(key);
                }
            }
        }
        return locations;
    }

    @Override
    public boolean hasResource(PackType packType, ResourceLocation location) {
        if (packType == PackType.CLIENT_RESOURCES) {
            if (images.containsKey(location)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> getNamespaces(PackType packType) {
        Set<String> namespaces = new HashSet<>();
        if (packType == PackType.CLIENT_RESOURCES) {
            for (ResourceLocation key : images.keySet()) {
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
        return PaletteExtractor.MOD_ID+"_generated";
    }

    @Override
    public void close() {

    }
}
