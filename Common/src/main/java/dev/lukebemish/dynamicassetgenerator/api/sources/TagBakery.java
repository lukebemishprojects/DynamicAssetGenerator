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
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/**
 * A utility for easily providing any number of tags to a resource cache.
 */
@SuppressWarnings("unused")
public class TagBakery implements PathAwareInputStreamSource, Resettable {
    private Map<ResourceLocation, Set<ResourceLocation>> bakedTags;
    private final List<TagSupplier> tagQueue;

    private final Map<ResourceLocation, Set<ResourceLocation>> staticQueue = new HashMap<>();

    public TagBakery() {
        this.tagQueue = new ArrayList<>();
        this.tagQueue.add(new TagSupplier() {
            @Override
            public Map<ResourceLocation, Set<ResourceLocation>> apply(ResourceGenerationContext context) {
                return staticQueue;
            }

            @Override
            public String createCacheKey(ResourceLocation outRl, ResourceGenerationContext context) {
                return "STATIC_QUEUE";
            }
        });
    }

    /**
     * Queues an unresolved set of tags to be added when this source is resolved.
     * @param tagSupplier supplies a map of tag identifiers to sets of registry entry identifiers
     */
    public void queue(TagSupplier tagSupplier) {
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
            checkTags(context);
            return build(bakedTags.get(outRl), context);
        };
    }

    private InputStream build(Set<ResourceLocation> paths, ResourceGenerationContext context) {
        StringBuilder internal = new StringBuilder();
        List<ResourceLocation> toAdd = new ArrayList<>(paths);
        toAdd.forEach(rl -> {
            if (!internal.isEmpty()) {
                internal.append(",\n");
            }
            internal.append("\"").append(rl.getNamespace()).append(":").append(rl.getPath()).append("\"");
        });
        String json = "{\n\"replace\":false,\n\"values\":["+internal+"\n]}";
        return new ByteArrayInputStream(json.getBytes());
    }

    @Override
    public @NotNull Set<ResourceLocation> getLocations(ResourceGenerationContext context) {
        checkTags(context);
        return bakedTags.keySet();
    }

    @Override
    public void reset(ResourceGenerationContext context) {
        bakedTags = null;
    }

    private void checkTags(ResourceGenerationContext context) {
        if (bakedTags == null) {
            bakedTags = new HashMap<>();
            tagQueue.forEach(function -> {
                Map<ResourceLocation, Set<ResourceLocation>> map = function.apply(context);
                map.forEach((tag, set) -> {
                    ResourceLocation tagFile = new ResourceLocation(tag.getNamespace(), "tags/" + tag.getPath() + ".json");
                    Set<ResourceLocation> entrySet = bakedTags.computeIfAbsent(tagFile, k -> new HashSet<>());
                    entrySet.addAll(set);
                });
            });
        }
    }

    @FunctionalInterface
    public interface TagSupplier extends Function<ResourceGenerationContext, Map<ResourceLocation,Set<ResourceLocation>>> {
        default @Nullable String createCacheKey(ResourceLocation outRl, ResourceGenerationContext context) {
            return null;
        }
    }

    @Override
    public @Nullable String createCacheKey(ResourceLocation outRl, ResourceGenerationContext context) {
        StringBuilder builder = new StringBuilder();
        for (TagSupplier tagSupplier : tagQueue) {
            String key = tagSupplier.createCacheKey(outRl, context);
            if (key == null) return null;
            builder.append(Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8)));
            builder.append('\n');
        }
        return builder.substring(0, builder.length() - 1);
    }
}
