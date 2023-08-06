/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.sources;

import dev.lukebemish.dynamicassetgenerator.api.PathAwareInputStreamSource;
import dev.lukebemish.dynamicassetgenerator.api.Resettable;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;

/**
 * A utility for easily providing any number of tags to a resource cache.
 */
@SuppressWarnings("unused")
public class TagBakery implements PathAwareInputStreamSource, Resettable {
    private Map<ResourceLocation, List<Supplier<Set<ResourceLocation>>>> bakedTags;
    private final List<Supplier<Map<ResourceLocation,Set<ResourceLocation>>>> tagQueue;

    private final Map<ResourceLocation, Set<ResourceLocation>> staticQueue = new HashMap<>();

    public TagBakery() {
        this.tagQueue = new ArrayList<>();
        this.tagQueue.add(() -> staticQueue);
    }

    /**
     * Queues an unresolved set of tags to be added when this source is resolved.
     * @param tagSupplier supplies a map of tag identifiers to sets of registry entry identifiers
     */
    public void queue(Supplier<Map<ResourceLocation,Set<ResourceLocation>>> tagSupplier) {
        this.tagQueue.add(tagSupplier);
    }

    /**
     * Queues the provided entries to be added to the provided tag when this source is resolved.
     * @param tag the identifier of the tag to be queued
     * @param entries the identifiers of the registry entries to be added to the tag
     */
    public void queue(ResourceLocation tag, Set<ResourceLocation> entries) {
        staticQueue.computeIfAbsent(tag, k -> new HashSet<>()).addAll(entries);
    }

    /**
     * Queues a single entry to be added to a tag when this source is resolved
     * @param tag the identifier of tag to add an entry to
     * @param entry the identifier of the entry to add
     */
    public void queue(ResourceLocation tag, ResourceLocation entry) {
        staticQueue.computeIfAbsent(tag, k -> new HashSet<>()).add(entry);
    }

    /**
     * Queues a set of tags to be added when this source is resolved.
     * @param tags a map of tag identifiers to sets of registry entry identifiers
     */
    public void queue(Map<ResourceLocation, Set<ResourceLocation>> tags) {
        tags.forEach((tag, entries) -> staticQueue.computeIfAbsent(tag, k -> new HashSet<>()).addAll(entries));
    }

    @Override
    public IoSupplier<InputStream> get(ResourceLocation outRl, ResourceGenerationContext context) {
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
            if (!internal.isEmpty()) {
                internal.append(",\n");
            }
            internal.append("    \"").append(rl.getNamespace()).append(":").append(rl.getPath()).append("\"");
        });
        String json = "{\n  \"replace\":false,\n  \"values\":["+internal+"\n]}";
        return new ByteArrayInputStream(json.getBytes());
    }

    @Override
    public @NotNull Set<ResourceLocation> getLocations(ResourceGenerationContext context) {
        checkTags();
        return bakedTags.keySet();
    }

    @Override
    public void reset(ResourceGenerationContext context) {
        bakedTags = null;
    }

    private void checkTags() {
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
