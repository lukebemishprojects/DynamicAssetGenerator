package io.github.lukebemish.dynamic_asset_generator.api.client;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.api.IPathAwareInputStreamSource;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Supplier;

public class GeneratedTextureHolder implements IPathAwareInputStreamSource {
    public static final Codec<GeneratedTextureHolder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("output_location").forGetter(dyn->dyn.outputLocation),
            ITexSource.TEXSOURCE_CODEC.fieldOf("input").forGetter(dyn->dyn.input)
    ).apply(instance, GeneratedTextureHolder::new));
    protected static final BiMap<ResourceLocation, Codec<? extends ITexSource>> SOURCES = HashBiMap.create();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();

    private final ResourceLocation outputLocation;
    private final ITexSource input;

    private Supplier<NativeImage> source;

    private GeneratedTextureHolder(ResourceLocation outputLocation, ITexSource source) {
        this.input = source;
        this.outputLocation = outputLocation;
    }

    public static Supplier<GeneratedTextureHolder> of(ResourceLocation outputLocation, ITexSource source) {
        GeneratedTextureHolder out = new GeneratedTextureHolder(outputLocation, source);
        return () -> {
            if (out.input != null && out.outputLocation != null) {
                Supplier<NativeImage> buffer = out.input.getSupplier();
                if (buffer == null) return null;
                out.source = buffer;
            } else {
                DynamicAssetGenerator.LOGGER.error("Could not set up GeneratedTextureHolder: {}", out);
            }
            return out;
        };
    }

    @Nullable
    public static GeneratedTextureHolder fromJson(String json) throws JsonSyntaxException {
        JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
        GeneratedTextureHolder out = CODEC.parse(JsonOps.INSTANCE, jsonObject).getOrThrow(false, s->{});
        if (out.input != null && out.outputLocation != null) {
            Supplier<NativeImage> buffer = out.input.getSupplier();
            if (buffer == null) return null;
            out.source = buffer;
        } else {
            DynamicAssetGenerator.LOGGER.error("Could not load JSON: {}", json);
        }
        return out;
    }

    public static void registerTexSourceReadingType(ResourceLocation rl, Codec<? extends ITexSource> reader) {
        SOURCES.put(rl, reader);
    }

    @Override
    public @NotNull Supplier<InputStream> get(ResourceLocation outRl) {
        if (this.source == null) return ()->null;
        return () -> {
            try (NativeImage image = source.get()) {
                if (image != null) {
                    return new ByteArrayInputStream(image.asByteArray());
                }
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Could not write image to stream: {}", outRl, e);
            } catch (JsonSyntaxException e) {
                DynamicAssetGenerator.LOGGER.error("Issue loading texture source JSON for output: {}", outRl, e);
            }
            return null;
        };
    }

    @Override
    public Set<ResourceLocation> location() {
        return Set.of(getOutputLocation());
    }

    public ResourceLocation getOutputLocation() {
        return new ResourceLocation(this.outputLocation.getNamespace(), "textures/"+this.outputLocation.getPath()+".png");
    }
}
