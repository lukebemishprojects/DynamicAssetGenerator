/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.VillagerMetaDataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A {@link ResourceGenerator} that generates a {@code .mcmeta} file for a texture by combining those of other textures
 * and/or setting properties manually.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class TextureMetaGenerator implements ResourceGenerator {
    public static final Codec<TextureMetaGenerator> CODEC = RecordCodecBuilder.create(i -> i.group(
        AnimationGenerator.CODEC.optionalFieldOf("animation", new AnimationGenerator.Builder().build()).forGetter(TextureMetaGenerator::getAnimation),
        VillagerGenerator.CODEC.optionalFieldOf("villager", new VillagerGenerator.Builder().build()).forGetter(TextureMetaGenerator::getVillager),
        TextureGenerator.CODEC.optionalFieldOf("texture", new TextureGenerator.Builder().build()).forGetter(TextureMetaGenerator::getTexture),
        ResourceLocation.CODEC.fieldOf("output_location").forGetter(TextureMetaGenerator::getOutputLocation),
        ResourceLocation.CODEC.listOf().fieldOf("sources").forGetter(TextureMetaGenerator::getSources)
    ).apply(i, TextureMetaGenerator::new));

    private final AnimationGenerator animation;
    private final VillagerGenerator villager;
    private final TextureGenerator texture;
    private final ResourceLocation outputLocation;
    private final List<ResourceLocation> sources;

    private TextureMetaGenerator(AnimationGenerator animation, VillagerGenerator villager, TextureGenerator texture, ResourceLocation outputLocation, List<ResourceLocation> sources) {
        this.animation = animation;
        this.villager = villager;
        this.texture = texture;
        this.outputLocation = outputLocation;
        this.sources = sources;
    }

    public AnimationGenerator getAnimation() {
        return animation;
    }

    public VillagerGenerator getVillager() {
        return villager;
    }

    public TextureGenerator getTexture() {
        return texture;
    }

    public ResourceLocation getOutputLocation() {
        return outputLocation;
    }

    public List<ResourceLocation> getSources() {
        return sources;
    }

    private static final Map<String, Function<TextureMetaGenerator, MetaSection>> SECTIONS = ImmutableMap.<String, Function<TextureMetaGenerator, MetaSection>>builder()
        .put(AnimationMetadataSection.SECTION_NAME, TextureMetaGenerator::getAnimation)
        .put(VillagerMetaDataSection.SECTION_NAME, TextureMetaGenerator::getVillager)
        .put("texture", TextureMetaGenerator::getTexture)
        .build();

    @Override
    public @NotNull <T> DataResult<T> persistentCacheData(DynamicOps<T> ops, ResourceLocation location, ResourceGenerationContext context) {
        var builder = ops.listBuilder();
        for (var s : sources) {
            ResourceLocation metaLocation = new ResourceLocation(s.getNamespace(), "textures/" + s.getPath() + ".png.mcmeta");
            var supplier = context.getResourceSource().getResource(metaLocation);
            if (supplier == null) {
                builder.add(ops.empty());
                continue;
            }
            try (var is = supplier.get()) {
                byte[] bytes = is.readAllBytes();
                String string = Base64.getEncoder().encodeToString(bytes);
                builder.add(ops.createString(string));
            } catch (IOException ignored) {
                return DataResult.error(() -> "Cannot cache potentially erroring source");
            }
        }
        return builder.build(ops.empty());
    }

    @Override
    public @Nullable IoSupplier<InputStream> get(ResourceLocation outRl, ResourceGenerationContext context) {
        if (sources.isEmpty()) {
            DynamicAssetGenerator.LOGGER.error("No sources provided for texture metadata at {}: ",outputLocation);
            return null;
        }

        return () -> {
            List<Pair<ResourceLocation, JsonObject>> originals = sources.stream().<Pair<ResourceLocation, JsonObject>>map(s -> {
                ResourceLocation metaLocation = new ResourceLocation(s.getNamespace(), "textures/" + s.getPath() + ".png.mcmeta");
                try {
                    var resource = context.getResourceSource().getResource(metaLocation);
                    if (resource == null) {
                        return Pair.of(s, null);
                    }
                    try (var reader = new BufferedReader(new InputStreamReader(resource.get()))) {
                        JsonObject meta = DynamicAssetGenerator.GSON.fromJson(reader, JsonObject.class);
                        return Pair.of(s, meta);
                    }
                } catch (Exception e) {
                    return Pair.of(s, null);
                }
            }).toList();

            var out = new JsonObject();

            for (var entry : SECTIONS.entrySet()) {
                var section = entry.getValue().apply(this);
                var member = section.apply(originals.stream().<Pair<ResourceLocation, JsonObject>>map(p -> {
                    var object = p.getSecond();
                    if (object == null) return new Pair<>(p.getFirst(), null);
                    var objectEntry = object.get(entry.getKey());
                    if (objectEntry == null || !objectEntry.isJsonObject()) return new Pair<>(p.getFirst(), null);
                    //noinspection Convert2Diamond
                    return new Pair<ResourceLocation, JsonObject>(p.getFirst(), objectEntry.getAsJsonObject());
                }).toList());
                if (member != null) {
                    out.add(entry.getKey(), member);
                }
            }

            return new ByteArrayInputStream(DynamicAssetGenerator.GSON_FLAT.toJson(out).getBytes(StandardCharsets.UTF_8));
        };
    }

    @Override
    public @NotNull Set<ResourceLocation> getLocations(ResourceGenerationContext context) {
        return Set.of(new ResourceLocation(outputLocation.getNamespace(),"textures/"+ outputLocation.getPath()+".png.mcmeta"));
    }

    @Override
    public Codec<? extends ResourceGenerator> codec() {
        return CODEC;
    }

    public static class Builder {
        private AnimationGenerator animation = new AnimationGenerator.Builder().build();
        private VillagerGenerator villager = new VillagerGenerator.Builder().build();
        private TextureGenerator texture = new TextureGenerator.Builder().build();
        private ResourceLocation outputLocation;
        private List<ResourceLocation> sources;

        /**
         * Set the animation data to use.
         */
        public Builder withAnimation(AnimationGenerator animation) {
            this.animation = animation;
            return this;
        }

        /**
         * Set the villager data to use.
         */
        public Builder withVillager(VillagerGenerator villager) {
            this.villager = villager;
            return this;
        }

        /**
         * Set the texture data to use.
         */
        public Builder withTexture(TextureGenerator texture) {
            this.texture = texture;
            return this;
        }

        /**
         * Set the location to output the generated {@code .mcmeta} file to, without the {@code "textures/"} prefix or
         * file extension.
         */
        public Builder withOutputLocation(ResourceLocation outputLocation) {
            this.outputLocation = outputLocation;
            return this;
        }

        /**
         * Set the list of textures, without their {@code "textures/"} prefix or file extension, to combine.
         */
        public Builder withSources(List<ResourceLocation> sources) {
            this.sources = sources;
            return this;
        }

        public TextureMetaGenerator build() {
            Objects.requireNonNull(outputLocation);
            Objects.requireNonNull(sources);
            return new TextureMetaGenerator(animation, villager, texture, outputLocation, sources);
        }
    }

    public interface MetaSection {
        @Nullable JsonObject apply(List<Pair<ResourceLocation, JsonObject>> originals);
    }

    public static class AnimationGenerator implements MetaSection {
        public static final Codec<AnimationGenerator> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("frametime").forGetter(AnimationGenerator::getFrametime),
            Codec.INT.optionalFieldOf("width").forGetter(AnimationGenerator::getWidth),
            Codec.INT.optionalFieldOf("height").forGetter(AnimationGenerator::getHeight),
            Codec.BOOL.optionalFieldOf("interpolate").forGetter(AnimationGenerator::getInterpolate)
        ).apply(i, AnimationGenerator::new));
        private final Optional<Integer> frametime;
        private final Optional<Integer> width;
        private final Optional<Integer> height;
        private final Optional<Boolean> interpolate;

        private AnimationGenerator(Optional<Integer> frametime, Optional<Integer> width, Optional<Integer> height, Optional<Boolean> interpolate) {
            this.frametime = frametime;
            this.width = width;
            this.height = height;
            this.interpolate = interpolate;
        }

        public Optional<Integer> getFrametime() {
            return frametime;
        }

        public Optional<Integer> getWidth() {
            return width;
        }

        public Optional<Integer> getHeight() {
            return height;
        }

        public Optional<Boolean> getInterpolate() {
            return interpolate;
        }

        public static class Builder {
            private Optional<Integer> frametime = Optional.empty();
            private Optional<Integer> width = Optional.empty();
            private Optional<Integer> height = Optional.empty();
            private Optional<Boolean> interpolate = Optional.empty();

            public Builder withFrametime(int frametime) {
                this.frametime = Optional.of(frametime);
                return this;
            }

            public Builder withWidth(int width) {
                this.width = Optional.of(width);
                return this;
            }

            public Builder withHeight(int height) {
                this.height = Optional.of(height);
                return this;
            }

            public Builder withInterpolate(boolean interpolate) {
                this.interpolate = Optional.of(interpolate);
                return this;
            }

            public AnimationGenerator build() {
                return new AnimationGenerator(frametime, width, height, interpolate);
            }
        }

        @Override
        public @Nullable JsonObject apply(List<Pair<ResourceLocation, JsonObject>> originals) {
            if (areAllMissing(originals) &&
                frametime.isEmpty() &&
                width.isEmpty() &&
                height.isEmpty() &&
                interpolate.isEmpty()
            ) return null;

            var originalsLocations = originals.stream().map(Pair::getFirst).toList();

            var parsed = originals.stream().map(p -> {
                var object = p.getSecond();
                if (object == null) return null;
                try {
                    return new Pair<>(p.getFirst(), AnimationMetadataSection.SERIALIZER.fromJson(object));
                } catch (Exception ignored) {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
            var out = new JsonObject();

            this.frametime.ifPresentOrElse(i -> out.addProperty("frametime", i), () ->
                tryForEach(originals, "frametime", JsonElement::isJsonPrimitive, member -> {
                    try {
                        out.addProperty("frametime", member.getAsInt());
                        return true;
                    } catch (UnsupportedOperationException | NumberFormatException ignored) {
                    }
                    return false;
                }));
            this.width.ifPresentOrElse(i -> out.addProperty("width", i), () ->
                tryForEach(originals, "width", JsonElement::isJsonPrimitive, member -> {
                    try {
                        out.addProperty("width", member.getAsInt());
                        return true;
                    } catch (UnsupportedOperationException | NumberFormatException ignored) {
                    }
                    return false;
                }));
            this.height.ifPresentOrElse(i -> out.addProperty("height", i), () ->
                tryForEach(originals, "height", JsonElement::isJsonPrimitive, member -> {
                    try {
                        out.addProperty("height", member.getAsInt());
                        return true;
                    } catch (UnsupportedOperationException | NumberFormatException ignored) {
                    }
                    return false;
                }));

            this.interpolate.ifPresentOrElse(i -> out.addProperty("interpolate", i), () ->
                tryForEach(originals, "interpolate", JsonElement::isJsonPrimitive, member -> {
                    try {
                        out.addProperty("interpolate", member.getAsBoolean());
                        return true;
                    } catch (UnsupportedOperationException ignored) {
                    }
                    return false;
                }));

            List<Frame> frames = new ArrayList<>();
            int frametime;
            try {
                frametime = out.getAsJsonPrimitive("frametime").getAsInt();
            } catch (Exception ignored) {
                frametime = 1;
            }
            frames.add(new Frame(0, frametime));

            for (ResourceLocation location : originalsLocations) {
                var section = parsed.get(location);
                if (section == null) continue;
                int maxFrames = getMaxFrames(section);
                frames = mutateList(frames, section, maxFrames);
            }

            if (frames.size() > 1) {
                JsonArray framesOut = new JsonArray();
                out.add("frames", framesOut);
                for (Frame frame : frames) {
                    JsonObject frameOut = new JsonObject();
                    frameOut.addProperty("index", frame.index());
                    frameOut.addProperty("time", frame.time());
                    framesOut.add(frameOut);
                }
            }

            return out;
        }

        private record Frame(int index, int time) {}

        private static int getMaxFrames(AnimationMetadataSection section) {
            AtomicInteger maxFrames = new AtomicInteger(0);
            section.forEachFrame((index, time) -> maxFrames.set(Math.max(maxFrames.get(), index)));
            return maxFrames.get() + 1;
        }

        private static List<Frame> mutateList(List<Frame> pattern, AnimationMetadataSection section, int maxFrames) {
            var out = new ArrayList<Frame>();
            for (Frame frame : pattern) {
                section.forEachFrame((index, time) -> {
                    int frameTime = time * frame.time / section.getDefaultFrameTime();
                    out.add(new Frame(frame.index * maxFrames + index, frameTime));
                });
            }
            return out;
        }
    }

    public static class VillagerGenerator implements MetaSection {
        public static final Codec<VillagerMetaDataSection.Hat> HAT_CODEC = Codec.STRING.flatXmap(s -> switch (s) {
            case "none" -> DataResult.success(VillagerMetaDataSection.Hat.NONE);
            case "partial" -> DataResult.success(VillagerMetaDataSection.Hat.PARTIAL);
            case "full" -> DataResult.success(VillagerMetaDataSection.Hat.FULL);
            default -> DataResult.error(() -> "Unknown hat type: " + s);
        }, h -> DataResult.success(getHatName(h)));

        private static String getHatName(VillagerMetaDataSection.Hat hat) {
            return switch (hat) {
                case NONE -> "none";
                case PARTIAL -> "partial";
                case FULL -> "full";
            };
        }

        public static final Codec<VillagerGenerator> CODEC = RecordCodecBuilder.create(i -> i.group(
            HAT_CODEC.optionalFieldOf("hat").forGetter(VillagerGenerator::getHat)
        ).apply(i, VillagerGenerator::new));

        private final Optional<VillagerMetaDataSection.Hat> hat;

        private VillagerGenerator(Optional<VillagerMetaDataSection.Hat> hat) {
            this.hat = hat;
        }

        public Optional<VillagerMetaDataSection.Hat> getHat() {
            return hat;
        }

        public static class Builder {
            private Optional<VillagerMetaDataSection.Hat> hat = Optional.empty();

            public Builder withHat(VillagerMetaDataSection.Hat hat) {
                this.hat = Optional.of(hat);
                return this;
            }

            public VillagerGenerator build() {
                return new VillagerGenerator(hat);
            }
        }

        @Override
        public @Nullable JsonObject apply(List<Pair<ResourceLocation, JsonObject>> originals) {
            if (areAllMissing(originals) && hat.isEmpty()) return null;

            var out = new JsonObject();
            this.hat.ifPresentOrElse(h -> out.addProperty("hat", getHatName(h)), () ->
                tryForEach(originals, "hat", JsonElement::isJsonPrimitive, member -> {
                    try {
                        out.addProperty("hat", member.getAsString());
                        return true;
                    } catch (UnsupportedOperationException ignored) {
                    }
                    return false;
                }));

            return out;
        }
    }

    public static class TextureGenerator implements MetaSection {
        public static final Codec<TextureGenerator> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("blur").forGetter(TextureGenerator::getBlur),
            Codec.BOOL.optionalFieldOf("clamp").forGetter(TextureGenerator::getClamp)
        ).apply(i, TextureGenerator::new));

        private final Optional<Boolean> blur;
        private final Optional<Boolean> clamp;

        private TextureGenerator(Optional<Boolean> blur, Optional<Boolean> clamp) {
            this.blur = blur;
            this.clamp = clamp;
        }

        public Optional<Boolean> getBlur() {
            return blur;
        }

        public Optional<Boolean> getClamp() {
            return clamp;
        }

        public static class Builder {
            private Optional<Boolean> blur = Optional.empty();
            private Optional<Boolean> clamp = Optional.empty();

            public Builder withBlur(boolean blur) {
                this.blur = Optional.of(blur);
                return this;
            }

            public Builder withClamp(boolean clamp) {
                this.clamp = Optional.of(clamp);
                return this;
            }

            public TextureGenerator build() {
                return new TextureGenerator(blur, clamp);
            }
        }

        @Override
        public @Nullable JsonObject apply(List<Pair<ResourceLocation, JsonObject>> originals) {
            if (areAllMissing(originals) && blur.isEmpty() && clamp.isEmpty()) return null;

            var out = new JsonObject();
            this.blur.ifPresentOrElse(b -> out.addProperty("blur", b), () ->
                tryForEach(originals, "blur", JsonElement::isJsonPrimitive, member -> {
                    try {
                        out.addProperty("blur", member.getAsBoolean());
                        return true;
                    } catch (UnsupportedOperationException ignored) {
                    }
                    return false;
                }));
            this.clamp.ifPresentOrElse(b -> out.addProperty("clamp", b), () ->
                tryForEach(originals, "clamp", JsonElement::isJsonPrimitive, member -> {
                    try {
                        out.addProperty("clamp", member.getAsBoolean());
                        return true;
                    } catch (UnsupportedOperationException ignored) {
                    }
                    return false;
                }));

            return out;
        }
    }

    private static void tryForEach(List<Pair<ResourceLocation, JsonObject>> originals, String key, Predicate<JsonElement> filter, Predicate<JsonElement> consumer) {
        for (Pair<ResourceLocation, JsonObject> original : originals) {
            if (original.getSecond() == null) continue;
            var member = original.getSecond().get(key);
            if (member != null && filter.test(member)) {
                if (consumer.test(member)) {
                    return;
                }
            }
        }
    }

    private static boolean areAllMissing(List<Pair<ResourceLocation, JsonObject>> originals) {
        for (Pair<ResourceLocation, JsonObject> original : originals) {
            if (original.getSecond() != null) return false;
        }
        return true;
    }
}
