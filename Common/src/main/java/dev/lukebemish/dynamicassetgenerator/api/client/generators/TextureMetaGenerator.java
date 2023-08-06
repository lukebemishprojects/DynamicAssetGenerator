/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.util.Maath;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

/**
 * A {@link ResourceGenerator} that generates a {@code .mcmeta} file for a texture by combining those of other textures
 * and/or setting properties manually.
 * @param sources a list of textures, without their {@code "textures/"} prefix or file extension, to combine
 * @param animation the animation data to use, if any
 * @param villager the villager data to use, if any
 * @param texture the texture data to use, if any
 * @param outputLocation the location to output the generated {@code .mcmeta} file to, without the {@code "textures/"}
 *                       prefix or file extension
 */
public record TextureMetaGenerator(List<ResourceLocation> sources, Optional<AnimationData> animation, Optional<VillagerData> villager, Optional<TextureData> texture, ResourceLocation outputLocation) implements ResourceGenerator {
    public static final Codec<TextureMetaGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().fieldOf("sources").forGetter(TextureMetaGenerator::sources),
            AnimationData.CODEC.optionalFieldOf("animation").forGetter(TextureMetaGenerator::animation),
            VillagerData.CODEC.optionalFieldOf("villager").forGetter(TextureMetaGenerator::villager),
            TextureData.CODEC.optionalFieldOf("texture").forGetter(TextureMetaGenerator::texture),
            ResourceLocation.CODEC.fieldOf("output_location").forGetter(TextureMetaGenerator::outputLocation)
    ).apply(instance,TextureMetaGenerator::new));

    /**
     * This constructor should be considered internal, but to avoid breaking backwards compatibility, no breaking changes
     * will be made until DynAssetGen 5.0.0 or later.
     */
    @ApiStatus.Internal
    public TextureMetaGenerator {

    }

    public static class Builder {
        private final List<ResourceLocation> sources = new ArrayList<>();
        private @Nullable AnimationData animation;
        private @Nullable VillagerData villager;
        private @Nullable TextureData texture;
        private ResourceLocation outputLocation;

        /**
         * @param location a texture, without its {@code "textures/"} prefix or file extension, to combine
         */
        public Builder withSource(ResourceLocation location) {
            this.sources.add(location);
            return this;
        }

        /**
         * @param locations a list of textures, without their {@code "textures/"} prefix or file extension, to combine
         */
        public Builder withSources(List<ResourceLocation> locations) {
            this.sources.addAll(locations);
            return this;
        }

        /**
         * @param animation the animation data to use
         */
        public Builder withAnimation(AnimationData animation) {
            this.animation = animation;
            return this;
        }

        /**
         * @param villager the villager data to use
         */
        public Builder withVillager(VillagerData villager) {
            this.villager = villager;
            return this;
        }

        /**
         * @param texture the texture data to use
         */
        public Builder withTexture(TextureData texture) {
            this.texture = texture;
            return this;
        }

        public TextureMetaGenerator build() {
            Objects.requireNonNull(outputLocation);
            if (sources.isEmpty()) {
                throw new IllegalStateException("At least one source must be provided");
            }
            return new TextureMetaGenerator(sources, Optional.ofNullable(animation), Optional.ofNullable(villager), Optional.ofNullable(texture), outputLocation);
        }

        /**
         * @param outputLocation the location to output the generated {@code .mcmeta} file to, without the
         *                       {@code "textures/"} prefix or file extension
         */
        public Builder withOutputLocation(ResourceLocation outputLocation) {
            this.outputLocation = outputLocation;
            return this;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    @Override
    public IoSupplier<InputStream> get(ResourceLocation outRl, ResourceGenerationContext context) {
        Map<ResourceLocation, MetaStructure> sourceStructure = new HashMap<>();
        if (sources().isEmpty()) {
            DynamicAssetGenerator.LOGGER.error("No sources provided for texture metadata at {}:\n",outputLocation());
            return null;
        }
        for (ResourceLocation source : sources()) {
            @Nullable var streamSupplier = context.getResourceSource().getResource(new ResourceLocation(source.getNamespace(),
                "textures/"+source.getPath()+".png.mcmeta"));
            if (streamSupplier == null) {
                sourceStructure.put(source,new MetaStructure(Optional.empty(),Optional.empty(),Optional.empty()));
            } else {
                try (InputStream stream = streamSupplier.get()) {
                    JsonObject json = GSON.fromJson(new BufferedReader(new InputStreamReader(stream)), JsonObject.class);
                    MetaStructure structure = MetaStructure.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, e -> {
                    });
                    sourceStructure.put(source, structure);
                } catch (IOException | RuntimeException e) {
                    //Not an error, it might just not have metadata
                    sourceStructure.put(source, new MetaStructure(Optional.empty(), Optional.empty(), Optional.empty()));
                }
            }
        }

        Optional<MetaStructure.AnimationMeta> animationMeta = Optional.empty();
        Optional<MetaStructure.VillagerMeta> villagerMeta = Optional.empty();
        Optional<MetaStructure.TextureMeta> textureMeta = Optional.empty();

        List<MetaStructure.AnimationMeta> animationMetas = sources().stream()
                .map(sourceStructure::get)
                .map(MetaStructure::animation)
                .filter(Optional::isPresent)
                .map(Optional::get).toList();
        List<MetaStructure.TextureMeta> textureMetas = sources().stream()
                .map(sourceStructure::get)
                .map(MetaStructure::texture)
                .filter(Optional::isPresent)
                .map(Optional::get).toList();
        List<MetaStructure.VillagerMeta> villagerMetas = sources().stream()
                .map(sourceStructure::get)
                .map(MetaStructure::villager)
                .filter(Optional::isPresent)
                .map(Optional::get).toList();

        if (!villagerMetas.isEmpty() || villager().isPresent()) {
            VillagerData.Hat hat = (villager().isPresent() ? villager().get().hat() : Optional.<VillagerData.Hat>empty())
                    .orElseGet(()->villagerMetas.get(0).hat());
            villagerMeta = Optional.of(new MetaStructure.VillagerMeta(hat));
        }

        if (!textureMetas.isEmpty() || texture().isPresent()) {
            boolean blur = (texture().isPresent() ? texture().get().blur() : Optional.<Boolean>empty())
                    .orElseGet(()->textureMetas.get(0).blur());
            boolean clamp = (texture().isPresent() ? texture().get().clamp() : Optional.<Boolean>empty())
                    .orElseGet(()->textureMetas.get(0).clamp());
            textureMeta = Optional.of(new MetaStructure.TextureMeta(blur, clamp));
        }

        if (!animationMetas.isEmpty()) {
            int frametime = (animation().isPresent() ? animation().get().frametime() : Optional.<Integer>empty())
                    .orElseGet(()->animationMetas.get(0).frametime());
            boolean interpolate = (animation().isPresent() ? animation().get().interpolate() : Optional.<Boolean>empty())
                    .orElseGet(()-> animationMetas.get(0).interpolate());

            List<Integer> frameCount = animationMetas.stream().map(m->m.frames.stream().max(Integer::compareTo).orElse(0)+1).toList();
            List<Integer> scale = new ArrayList<>(animation().map(AnimationData::scales).map(l->l.orElse(List.of())).orElse(List.of()));
            List<Integer> relFrameCount = new ArrayList<>();

            Supplier<ResourceLocation> rlFinder = () -> sources().stream()
                    .filter(i->sourceStructure.get(i).animation().map(m->!m.frames.equals(List.of(0))).orElse(false))
                    .findFirst().orElse(sources().get(0));
            ResourceLocation patternSourceRl = animation().map(AnimationData::patternSource)
                    .map(s->s.orElseGet(rlFinder)).orElseGet(rlFinder);
            while (scale.size() < sources().size())
                scale.add(1);

            for (int i = 0; i < frameCount.size(); i++)
                relFrameCount.add(frameCount.get(i) * scale.get(i));
            int totalLength = Maath.lcm(relFrameCount);

            if (!sources.contains(patternSourceRl)) {
                DynamicAssetGenerator.LOGGER.error("Source specified was not the name of a texture source: {}",patternSourceRl);
                return null;
            }

            List<Integer> framesSource = sourceStructure.get(patternSourceRl).animation.map(MetaStructure.AnimationMeta::frames).orElse(List.of(0));
            int patternSourceIdx = sourceStructure.get(patternSourceRl).animation.map(animationMetas::indexOf).orElse(0);
            List<Integer> framesOut = new ArrayList<>();
            int scalingFactor = totalLength / frameCount.get(patternSourceIdx);
            for (int f : framesSource) {
                for (int i = 0; i < scalingFactor; i++) {
                    framesOut.add(f*scalingFactor+i);
                }
            }

            animationMeta = Optional.of(new MetaStructure.AnimationMeta(frametime, framesOut, interpolate));
        }

        MetaStructure out = new MetaStructure(animationMeta, textureMeta, villagerMeta);

        JsonElement jsonOut = MetaStructure.CODEC.encodeStart(JsonOps.INSTANCE, out).getOrThrow(false, e->{});
        return () -> new ByteArrayInputStream(GSON.toJson(jsonOut).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public @NotNull Set<ResourceLocation> getLocations(ResourceGenerationContext context) {
        return Set.of(new ResourceLocation(outputLocation().getNamespace(),"textures/"+outputLocation().getPath()+".png.mcmeta"));
    }

    @Override
    public Codec<? extends ResourceGenerator> codec() {
        return CODEC;
    }

    /**
     * Represents the animation-related metadata of a texture
     * @param frametime the time in ticks between each frame
     * @param interpolate whether to interpolate between frames
     * @param patternSource which of the provided textures determines the main order of frames in the animation.
     * @param scales the scaling factor for each texture's animation
     */
    public record AnimationData(Optional<Integer> frametime, Optional<Boolean> interpolate, Optional<ResourceLocation> patternSource, Optional<List<Integer>> scales) {
        public static final Codec<AnimationData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("frametime").forGetter(AnimationData::frametime),
                Codec.BOOL.optionalFieldOf("interpolate").forGetter(AnimationData::interpolate),
                ResourceLocation.CODEC.optionalFieldOf("pattern_source").forGetter(AnimationData::patternSource),
                Codec.INT.listOf().optionalFieldOf("scales").forGetter(AnimationData::scales)
        ).apply(instance,AnimationData::new));
    }

    /**
     * Represents the villager-related metadata of a texture
     * @param hat how much of the villager's hat is visible
     */
    public record VillagerData(Optional<Hat> hat) {
        public static final Codec<VillagerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Hat.CODEC.optionalFieldOf("frametime").forGetter(VillagerData::hat)
        ).apply(instance,VillagerData::new));

        /**
         * Represents the amount of a villager's hat that is visible
         */
        public enum Hat implements StringRepresentable {
            NONE("none"),
            PARTIAL("partial"),
            FULL("full");

            public static final Codec<Hat> CODEC = StringRepresentable.fromEnum(Hat::values);

            private final String string;
            Hat(String string) {
                this.string = string;
            }

            @Override
            public @NotNull String getSerializedName() {
                return string;
            }
        }
    }

    /**
     * Represents the texture-related metadata of a texture
     * @param blur whether to blur the texture
     * @param clamp whether to clamp the texture
     */
    public record TextureData(Optional<Boolean> blur, Optional<Boolean> clamp) {
        public static final Codec<TextureData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("blur").forGetter(TextureData::blur),
                Codec.BOOL.optionalFieldOf("clamp").forGetter(TextureData::clamp)
        ).apply(instance,TextureData::new));
    }

    private record MetaStructure(Optional<AnimationMeta> animation, Optional<TextureMeta> texture, Optional<VillagerMeta> villager) {
        public static final Codec<MetaStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                AnimationMeta.CODEC.optionalFieldOf("animation").forGetter(MetaStructure::animation),
                TextureMeta.CODEC.optionalFieldOf("texture").forGetter(MetaStructure::texture),
                VillagerMeta.CODEC.optionalFieldOf("villager").forGetter(MetaStructure::villager)
        ).apply(instance, MetaStructure::new));
        record AnimationMeta(int frametime, List<Integer> frames, boolean interpolate) {
            public static final Codec<AnimationMeta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("frametime",1).forGetter(AnimationMeta::frametime),
                    Codec.INT.listOf().optionalFieldOf("frames",List.of(0)).forGetter(AnimationMeta::frames),
                    Codec.BOOL.optionalFieldOf("interpolate",false).forGetter(AnimationMeta::interpolate)
            ).apply(instance,AnimationMeta::new));
        }
        record TextureMeta(boolean blur, boolean clamp) {
            public static final Codec<TextureMeta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("blur",false).forGetter(TextureMeta::blur),
                    Codec.BOOL.optionalFieldOf("clamp", false).forGetter(TextureMeta::clamp)
            ).apply(instance,TextureMeta::new));
        }
        record VillagerMeta(VillagerData.Hat hat) {
            public static final Codec<VillagerMeta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    VillagerData.Hat.CODEC.optionalFieldOf("hat", VillagerData.Hat.NONE).forGetter(VillagerMeta::hat)
            ).apply(instance,VillagerMeta::new));
        }
    }
}
