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
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.util.Maath;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.VillagerMetaDataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
                        DynamicAssetGenerator.LOGGER.error("Failed to load texture metadata source: {}",s);
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
            Codec.INT.listOf().optionalFieldOf("scales").forGetter(AnimationGenerator::getScales),
            Codec.INT.optionalFieldOf("frametime").forGetter(AnimationGenerator::getFrametime),
            Codec.INT.optionalFieldOf("width").forGetter(AnimationGenerator::getWidth),
            Codec.INT.optionalFieldOf("height").forGetter(AnimationGenerator::getHeight),
            Codec.BOOL.optionalFieldOf("interpolate").forGetter(AnimationGenerator::getInterpolate),
            ResourceLocation.CODEC.optionalFieldOf("pattern").forGetter(AnimationGenerator::getPatternSource)
        ).apply(i, AnimationGenerator::new));

        private final Optional<List<Integer>> scales;
        private final Optional<Integer> frametime;
        private final Optional<Integer> width;
        private final Optional<Integer> height;
        private final Optional<Boolean> interpolate;
        private final Optional<ResourceLocation> patternSource;

        private AnimationGenerator(Optional<List<Integer>> scales, Optional<Integer> frametime, Optional<Integer> width, Optional<Integer> height, Optional<Boolean> interpolate, Optional<ResourceLocation> patternSource) {
            this.scales = scales;
            this.frametime = frametime;
            this.width = width;
            this.height = height;
            this.interpolate = interpolate;
            this.patternSource = patternSource;
        }

        public Optional<List<Integer>> getScales() {
            return scales;
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

        public Optional<ResourceLocation> getPatternSource() {
            return patternSource;
        }

        public static class Builder {
            private Optional<List<Integer>> scales = Optional.empty();
            private Optional<Integer> frametime = Optional.empty();
            private Optional<Integer> width = Optional.empty();
            private Optional<Integer> height = Optional.empty();
            private Optional<Boolean> interpolate = Optional.empty();
            private Optional<ResourceLocation> patternSource = Optional.empty();

            public Builder withScales(List<Integer> scales) {
                this.scales = Optional.of(scales);
                return this;
            }

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

            public Builder withPatternSource(ResourceLocation patternSource) {
                this.patternSource = Optional.of(patternSource);
                return this;
            }

            public AnimationGenerator build() {
                return new AnimationGenerator(scales, frametime, width, height, interpolate, patternSource);
            }
        }

        @Override
        public @Nullable JsonObject apply(List<Pair<ResourceLocation, JsonObject>> originals) {
            // TODO: Figure out the weird frame objects that have a "index" and "time" field.

            if (areAllMissing(originals) &&
                frametime.isEmpty() &&
                width.isEmpty() &&
                height.isEmpty() &&
                interpolate.isEmpty()
            ) return null;

            var originalsMap = originals.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
            var originalsLocations = originals.stream().map(Pair::getFirst).toList();
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

            ResourceLocation patternSource = this.patternSource.orElseGet(() -> originals.stream().filter(pair -> {
                var frametimes = pair.getSecond().get("frames");
                return frametimes != null && frametimes.isJsonArray();
            }).map(Pair::getFirst).findFirst().orElse(originals.get(0).getFirst()));

            List<Integer> scale = new ArrayList<>(scales.orElse(List.of()));
            while (scale.size() < originals.size())
                scale.add(1);

            List<Integer> frameCount = originals.stream().map(m-> {
                var frames = m.getSecond().get("frames");
                if (frames != null && frames.isJsonArray()) {
                    var maximum = 0;
                    for (JsonElement frame : frames.getAsJsonArray()) {
                        try {
                            var index = frame.getAsInt();
                            if (index > maximum) {
                                maximum = index;
                            }
                        } catch (UnsupportedOperationException | NumberFormatException ignored) {
                        }
                    }
                    return maximum + 1;
                }
                return 1;
            }).toList();

            List<Integer> relFrameCount = new ArrayList<>();

            for (int i = 0; i < frameCount.size(); i++)
                relFrameCount.add(frameCount.get(i) * scale.get(i));
            int totalLength = Maath.lcm(relFrameCount);

            if (!originalsMap.containsKey(patternSource)) {
                DynamicAssetGenerator.LOGGER.error("Source specified was not the name of a texture source: {}",patternSource);
                return null;
            }

            List<Integer> framesSource;
            var member = originalsMap.get(patternSource).get("frames");
            if (member != null && member.isJsonArray()) {
                var tempFramesSource = new ArrayList<Integer>();
                for (JsonElement frame : member.getAsJsonArray()) {
                    try {
                        tempFramesSource.add(frame.getAsInt());
                    } catch (UnsupportedOperationException | NumberFormatException ignored) {
                        framesSource = List.of(0);
                        break;
                    }
                }
                framesSource = tempFramesSource;
            } else {
                framesSource = List.of(0);
            }

            int patternSourceIdx = originalsLocations.indexOf(patternSource);

            JsonArray framesOut = new JsonArray();
            int scalingFactor = totalLength / frameCount.get(patternSourceIdx);
            for (int f : framesSource) {
                for (int i = 0; i < scalingFactor; i++) {
                    framesOut.add(f*scalingFactor+i);
                }
            }

            out.add("frames", framesOut);

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
            if (original.getSecond() == null) continue;
            return true;
        }
        return true;
    }
}
