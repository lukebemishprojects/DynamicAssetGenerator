package io.github.lukebemish.dynamic_asset_generator.api.client.generators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.api.IResourceGenerator;
import io.github.lukebemish.dynamic_asset_generator.api.client.ClientPrePackRepository;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.texsources.AnimationSplittingSource;
import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public record TextureMetaGenerator(List<MetaSource> sources, Optional<AnimationData> animation, Optional<VillagerData> villager, Optional<TextureData> texture, ResourceLocation outputLocation) implements IResourceGenerator {
    public static final Codec<TextureMetaGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MetaSource.CODEC.listOf().fieldOf("sources").forGetter(TextureMetaGenerator::sources),
            AnimationData.CODEC.optionalFieldOf("animation").forGetter(TextureMetaGenerator::animation),
            VillagerData.CODEC.optionalFieldOf("villager").forGetter(TextureMetaGenerator::villager),
            TextureData.CODEC.optionalFieldOf("texture").forGetter(TextureMetaGenerator::texture),
            ResourceLocation.CODEC.fieldOf("output_location").forGetter(TextureMetaGenerator::outputLocation)
    ).apply(instance,TextureMetaGenerator::new));

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    @Override
    public @NotNull Supplier<InputStream> get(ResourceLocation outRl) {
        return () -> {
            Map<ResourceLocation, MetaSource> sourceMap = new HashMap<>();
            Map<ResourceLocation, MetaStructure> sourceStructure = new HashMap<>();
            for (MetaSource source : sources()) {
                try (InputStream stream = ClientPrePackRepository.getResource(source.texture())){
                    JsonObject json = GSON.fromJson(new BufferedReader(new InputStreamReader(stream)), JsonObject.class);
                    MetaStructure structure = MetaStructure.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, e->{});
                    sourceStructure.put(source.texture(),structure);
                    sourceMap.put(source.texture(),source);
                } catch (IOException | RuntimeException e) {
                    DynamicAssetGenerator.LOGGER.error("Issue reading texture metadata for {}:\n",source.texture(),e);
                    return null;
                }
            }

            Optional<MetaStructure.AnimationMeta> animationMeta = Optional.empty();
            Optional<MetaStructure.VillagerMeta> villagerMeta = Optional.empty();
            Optional<MetaStructure.TextureMeta> textureMeta = Optional.empty();

            Map<ResourceLocation, MetaStructure.AnimationMeta> animationMetas = new HashMap<>();
            sources().forEach(source -> {
                Optional<MetaStructure.AnimationMeta> meta = sourceStructure.get(source.texture()).animation();
                meta.ifPresent(value -> animationMetas.put(source.texture(), value));
            });
            Set<MetaStructure.TextureMeta> textureMetas = sourceStructure.values().stream().map(MetaStructure::texture)
                    .filter(Optional::isPresent)
                    .map(Optional::get).collect(Collectors.toSet());
            Set<MetaStructure.VillagerMeta> villagerMetas = sourceStructure.values().stream().map(MetaStructure::villager)
                    .filter(Optional::isPresent)
                    .map(Optional::get).collect(Collectors.toSet());

            if (!villagerMetas.isEmpty() || villager().isPresent()) {
                VillagerData.Hat hat = (villager().isPresent() ? villager().get().hat() : Optional.<VillagerData.Hat>empty())
                        .orElseGet(()->villagerMetas.stream().findFirst().get().hat());
                villagerMeta = Optional.of(new MetaStructure.VillagerMeta(hat));
            }

            if (!textureMetas.isEmpty() || texture().isPresent()) {
                boolean blur = (texture().isPresent() ? texture().get().blur() : Optional.<Boolean>empty())
                        .orElseGet(()->textureMetas.stream().findFirst().get().blur());
                boolean clamp = (texture().isPresent() ? texture().get().clamp() : Optional.<Boolean>empty())
                        .orElseGet(()->textureMetas.stream().findFirst().get().clamp());
                textureMeta = Optional.of(new MetaStructure.TextureMeta(blur, clamp));
            }

            if (!animationMetas.isEmpty() || animation().isPresent()) {
                int frametime = (animation().isPresent() ? animation().get().frametime() : Optional.<Integer>empty())
                        .orElseGet(()->animationMetas.values().stream().findFirst().get().frametime());
                boolean interpolate = (animation().isPresent() ? animation().get().interpolate() : Optional.<Boolean>empty())
                        .orElseGet(()-> animationMetas.values().stream().anyMatch(MetaStructure.AnimationMeta::interpolate));
                int lcm = AnimationSplittingSource.lcm(animationMetas.entrySet().stream().map(e->{
                    List<Integer> frames = e.getValue().frames();
                    return (frames.isEmpty() ? 1 : frames.size())*sourceMap.get(e.getKey()).scale();
                }).toList());
                ArrayList<Integer> frames = new ArrayList<>();
                for (int i = 1; i<=lcm; i++) {
                    frames.add(i);
                }
                animationMeta = Optional.of(new MetaStructure.AnimationMeta(frametime, frames, interpolate));
            }

            MetaStructure out = new MetaStructure(animationMeta, textureMeta, villagerMeta);

            JsonElement jsonOut = MetaStructure.CODEC.encodeStart(JsonOps.INSTANCE, out).getOrThrow(false, e->{});
            return new ByteArrayInputStream(GSON.toJson(jsonOut).getBytes(StandardCharsets.UTF_8));
        };
    }

    @Override
    public @NotNull Set<ResourceLocation> location() {
        return Set.of(outputLocation());
    }

    @Override
    public Codec<? extends IResourceGenerator> codec() {
        return CODEC;
    }

    public record MetaSource(ResourceLocation texture, int scale) {
        public static final Codec<MetaSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("texture").forGetter(MetaSource::texture),
                Codec.INT.optionalFieldOf("animation_scale",1).forGetter(MetaSource::scale)
        ).apply(instance, MetaSource::new));
    }

    public record AnimationData(Optional<Integer> frametime, Optional<Boolean> interpolate) {
        public static final Codec<AnimationData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("frametime").forGetter(AnimationData::frametime),
                Codec.BOOL.optionalFieldOf("interpolate").forGetter(AnimationData::interpolate)
        ).apply(instance,AnimationData::new));
    }

    public record VillagerData(Optional<Hat> hat) {
        public static final Codec<VillagerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Hat.CODEC.optionalFieldOf("frametime").forGetter(VillagerData::hat)
        ).apply(instance,VillagerData::new));

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
            public String getSerializedName() {
                return string;
            }
        }
    }

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
                    Codec.INT.listOf().optionalFieldOf("frames",List.of()).forGetter(AnimationMeta::frames),
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
