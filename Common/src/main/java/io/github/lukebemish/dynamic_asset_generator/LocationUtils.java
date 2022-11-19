package io.github.lukebemish.dynamic_asset_generator;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("unused")
public final class LocationUtils {
    private LocationUtils() {}

    public static ResourceLocation withPrefix(ResourceLocation location, String prefix) {
        if (prefix.isEmpty())
            return location;
        return new ResourceLocation(location.getNamespace(), prefix + "/" + location.getPath());
    }

    public static ResourceLocation withExtension(ResourceLocation location, String extension) {
        if (extension.isEmpty())
            return location;
        return new ResourceLocation(location.getNamespace(), location.getPath() + "." + extension);
    }

    public static Pair<String, ResourceLocation> withoutPrefix(ResourceLocation location) {
        String[] parts = location.getPath().split("/", 2);
        if (parts.length == 1)
            return Pair.of("", location);
        return new Pair<>(parts[0], new ResourceLocation(location.getNamespace(), parts[1]));
    }

    public static Pair<String, ResourceLocation> withoutExtension(ResourceLocation location) {
        int index = location.getPath().lastIndexOf('.');
        if (index == -1)
            return Pair.of("", location);
        return new Pair<>(location.getPath().substring(index + 1), new ResourceLocation(location.getNamespace(), location.getPath().substring(0, index)));
    }
}
