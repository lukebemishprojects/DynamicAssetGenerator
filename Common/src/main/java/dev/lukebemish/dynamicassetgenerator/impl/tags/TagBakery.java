/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.tags;

import dev.lukebemish.dynamicassetgenerator.api.IPathAwareInputStreamSource;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;

public class TagBakery implements IPathAwareInputStreamSource {
    private Map<ResourceLocation, List<Supplier<Set<ResourceLocation>>>> bakedTags;
    private final List<Supplier<Map<ResourceLocation,Set<ResourceLocation>>>> tagQueue;

    public TagBakery(List<Supplier<Map<ResourceLocation,Set<ResourceLocation>>>> tagQueue) {
        this.tagQueue = tagQueue;
    }

    @Override
    public @NotNull Supplier<InputStream> get(ResourceLocation outRl) {
        return () -> {
            checkTags();
            return build(bakedTags.get(outRl));
        };
    }

    private InputStream build(List<Supplier<Set<ResourceLocation>>> paths) {
        StringBuilder internal = new StringBuilder();
        List<ResourceLocation> toAdd = new ArrayList<>();
        for (Supplier<Set<ResourceLocation>> p : paths) {
            toAdd.addAll(p.get());
        }
        toAdd.forEach(rl -> {
            if (internal.length() >= 1) {
                internal.append(",\n");
            }
            internal.append("    \"").append(rl.getNamespace()).append(":").append(rl.getPath()).append("\"");
        });
        String json = "{\n  \"replace\":false,\n  \"values\":["+internal+"\n]}";
        return new ByteArrayInputStream(json.getBytes());
    }

    @Override
    public @NotNull Set<ResourceLocation> getLocations() {
        checkTags();
        return bakedTags.keySet();
    }

    public void reset() {
        bakedTags = null;
    }

    public void checkTags() {
        if (bakedTags == null) {
            bakedTags = new HashMap<>();
            tagQueue.forEach(supplier -> {
                Map<ResourceLocation, Set<ResourceLocation>> map = supplier.get();
                map.forEach((tag, set) -> {
                    ResourceLocation tagFile = new ResourceLocation(tag.getNamespace(), "tags/" + tag.getPath() + ".json");
                    List<Supplier<Set<ResourceLocation>>> list = bakedTags.computeIfAbsent(tagFile, k -> new ArrayList<>());
                    list.add(() -> set);
                });
            });

        }
    }
}
