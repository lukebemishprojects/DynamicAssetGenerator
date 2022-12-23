/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("unused")
public final class LocationUtils {
    private LocationUtils() {}

    /**
     * Prefixes a location with a prefix within the same namespace.
     * @param location The location to prefix.
     * @param prefix The prefix to add.
     * @return The prefixed location.
     */
    public static ResourceLocation withPrefix(ResourceLocation location, String prefix) {
        if (prefix.isEmpty())
            return location;
        return new ResourceLocation(location.getNamespace(), prefix + "/" + location.getPath());
    }

    /**
     * Adds an extension to a location.
     * @param location The location to add the extension to.
     * @param extension The extension to add.
     * @return The location with the extension added.
     */
    public static ResourceLocation withExtension(ResourceLocation location, String extension) {
        if (extension.isEmpty())
            return location;
        return new ResourceLocation(location.getNamespace(), location.getPath() + "." + extension);
    }

    /**
     * Separates a prefixed location into its first prefix and the rest of the location.
     * @param location The location to separate.
     * @return A pair of the first prefix and the rest of the location.
     */
    public static Pair<String, ResourceLocation> separatePrefix(ResourceLocation location) {
        String[] parts = location.getPath().split("/", 2);
        if (parts.length == 1)
            return Pair.of("", location);
        return new Pair<>(parts[0], new ResourceLocation(location.getNamespace(), parts[1]));
    }

    /**
     * Separates the extension of a location from the rest of the path.
     * @param location The location to separate.
     * @return A pair of the extension and the location without the extension.
     */
    public static Pair<String, ResourceLocation> separateExtension(ResourceLocation location) {
        int index = location.getPath().lastIndexOf('.');
        if (index == -1)
            return Pair.of("", location);
        return new Pair<>(location.getPath().substring(index + 1), new ResourceLocation(location.getNamespace(), location.getPath().substring(0, index)));
    }
}
