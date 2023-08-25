/*
 * Copyright (C) 2023 Luke Bemish and contributors
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
 * Can supplier a map of tags to contents given a generation context
 */
@FunctionalInterface
public interface TagSupplier extends Function<ResourceGenerationContext, Map<ResourceLocation, Set<ResourceLocation>>>, Resettable {

    /**
     * Create a key that can be <em>uniquely</em> used to identify the tag map to be generated. Note that this is used
     * for caching across reloads, and so should incorporate any resources that may be used to generate the resource. If
     * this is not possible, return null.
     * @param outRl the resource location that will be generated
     * @param context the context that the resource will be generated in. Resources can safely be accessed in this context
     * @return a key that can be used to uniquely identify the resource, or null if this is not possible
     */
    default @Nullable String createSupplierCacheKey(ResourceLocation outRl, ResourceGenerationContext context) {
        return null;
    }

    @Override
    default void reset(ResourceGenerationContext context) {}

    /**
     * A utility for easily providing any number of tags to a resource cache.
     */
    @SuppressWarnings("unused")
    class TagBakery implements PathAwareInputStreamSource, Resettable, TagSupplier {
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
                public String createSupplierCacheKey(ResourceLocation outRl, ResourceGenerationContext context) {
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
            if (!outRl.getPath().startsWith("tags/") || !outRl.getPath().endsWith(".json")) {
                return null;
            }
            return () -> {
                checkTags(context);
                ResourceLocation tagRl = new ResourceLocation(outRl.getNamespace(), outRl.getPath().substring(5, outRl.getPath().length() - 5));
                return build(bakedTags.get(tagRl));
            };
        }

        private InputStream build(Set<ResourceLocation> paths) {
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
            for (TagSupplier tagSupplier : tagQueue) {
                tagSupplier.reset(context);
            }
        }

        private synchronized void checkTags(ResourceGenerationContext context) {
            if (bakedTags == null) {
                bakedTags = new HashMap<>();
                tagQueue.forEach(function -> {
                    Map<ResourceLocation, Set<ResourceLocation>> map = function.apply(context);
                    map.forEach((tag, set) -> {
                        Set<ResourceLocation> entrySet = bakedTags.computeIfAbsent(tag, k -> new HashSet<>());
                        entrySet.addAll(set);
                    });
                });
            }
        }

        @Override
        public Map<ResourceLocation, Set<ResourceLocation>> apply(ResourceGenerationContext context) {
            checkTags(context);
            return bakedTags;
        }

        @Override
        public @Nullable String createCacheKey(ResourceLocation outRl, ResourceGenerationContext context) {
            StringBuilder builder = new StringBuilder();
            for (TagSupplier tagSupplier : tagQueue) {
                String key = tagSupplier.createSupplierCacheKey(outRl, context);
                if (key == null) return null;
                builder.append(Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8)));
                builder.append('\n');
            }
            return builder.substring(0, builder.length() - 1);
        }

        @Override
        public @Nullable String createSupplierCacheKey(ResourceLocation outRl, ResourceGenerationContext context) {
            return createCacheKey(outRl, context);
        }
    }
}
