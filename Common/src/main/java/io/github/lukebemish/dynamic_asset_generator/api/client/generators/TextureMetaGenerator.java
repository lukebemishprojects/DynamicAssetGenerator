package io.github.lukebemish.dynamic_asset_generator.api.client.generators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.api.IResourceGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public record TextureMetaGenerator(List<AnimationSource> sources, Optional<AnimationData> animation, ResourceLocation texture) implements IResourceGenerator {
    public static final Codec<TextureMetaGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AnimationSource.CODEC.listOf().fieldOf("sources").forGetter(TextureMetaGenerator::sources),
            AnimationData.CODEC.optionalFieldOf("animation").forGetter(TextureMetaGenerator::animation),
            ResourceLocation.CODEC.fieldOf("texture").forGetter(TextureMetaGenerator::texture)
    ).apply(instance,TextureMetaGenerator::new));

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    @Override
    public @NotNull Supplier<InputStream> get(ResourceLocation outRl) {
        return () -> {

        }
    }

    @Override
    public @NotNull Set<ResourceLocation> location() {
        return Set.of(texture());
    }

    @Override
    public Codec<? extends IResourceGenerator> codec() {
        return CODEC;
    }

    public record AnimationSource(ResourceLocation texture, int scale) {
        public static final Codec<AnimationSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("texture").forGetter(AnimationSource::texture),
                Codec.INT.optionalFieldOf("animation_scale",1).forGetter(AnimationSource::scale)
        ).apply(instance, AnimationSource::new));
    }

    public record AnimationData(Optional<Integer> frametime, Optional<Boolean> interpolate) {
        public static final Codec<AnimationData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("frametime").forGetter(AnimationData::frametime),
                Codec.BOOL.optionalFieldOf("interpolate").forGetter(AnimationData::interpolate)
        ).apply(instance,AnimationData::new));
    }

    private record MetaOutput() {
        record AnimationMeta(int frametime, List<Integer> frames, boolean interpolate) {
            public static final Codec<AnimationMeta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("frametime",1).forGetter(AnimationMeta::frametime),
                    Codec.INT.listOf().fieldOf("frames").forGetter(AnimationMeta::frames),
                    Codec.BOOL.optionalFieldOf("interpolate",false).forGetter(AnimationMeta::interpolate)
            ).apply(instance,AnimationMeta::new));
        }
        record TextureMeta(boolean blur, boolean clamp) {
            public static final Codec<TextureMeta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("blur",false).forGetter(TextureMeta::blur),
                    Codec.BOOL.optionalFieldOf("clamp", false).forGetter(TextureMeta::clamp)
            ).apply(instance,TextureMeta::new));
        }
        record VillagerMeta(Hat hat) {
            public static final Codec<VillagerMeta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Hat.CODEC.optionalFieldOf("hat",Hat.NONE).forGetter(VillagerMeta::hat)
            ).apply(instance,VillagerMeta::new));
            enum Hat implements StringRepresentable {
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
    }
}
