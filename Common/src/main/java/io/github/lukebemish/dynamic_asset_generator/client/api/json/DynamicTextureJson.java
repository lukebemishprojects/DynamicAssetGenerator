package io.github.lukebemish.dynamic_asset_generator.client.api.json;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class DynamicTextureJson {
    public static final Codec<ITexSource> TEXSOURCE_CODEC = ExtraCodecs.lazyInitializedCodec(() -> new Codec<Codec<? extends ITexSource>>() {
        @Override
        public <T> DataResult<Pair<Codec<? extends ITexSource>, T>> decode(DynamicOps<T> ops, T input) {
            return ResourceLocation.CODEC.decode(ops, input).flatMap(keyValuePair -> !DynamicTextureJson.SOURCES.containsKey(keyValuePair.getFirst())
                    ? DataResult.error("Unknown dynamic texture source type: " + keyValuePair.getFirst())
                    : DataResult.success(keyValuePair.mapFirst(DynamicTextureJson.SOURCES::get)));
        }

        @Override
        public <T> DataResult<T> encode(Codec<? extends ITexSource> input, DynamicOps<T> ops, T prefix) {
            ResourceLocation key = DynamicTextureJson.SOURCES.inverse().get(input);
            if (key == null)
            {
                return DataResult.error("Unregistered dynamic texture source type: " + input);
            }
            T toMerge = ops.createString(key.toString());
            return ops.mergeToPrimitive(prefix, toMerge);
        }
    }).dispatch(ITexSource::codec, Function.identity());

    public static final Codec<DynamicTextureJson> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("output_location").forGetter(dyn->dyn.outputLocation),
            TEXSOURCE_CODEC.fieldOf("input").forGetter(dyn->dyn.input)
    ).apply(instance,DynamicTextureJson::new));

    private static final Map<ResourceLocation, ITexSource> sources = new HashMap<>();
    private static final BiMap<ResourceLocation, Codec<? extends ITexSource>> SOURCES = HashBiMap.create();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();

    public String outputLocation;
    public ITexSource input;

    public Supplier<NativeImage> source;

    public DynamicTextureJson(String outputLocation, ITexSource source) {
        this.input = source;
        this.outputLocation = outputLocation;
    }

    @Nullable
    public static DynamicTextureJson fromJson(String json) throws JsonSyntaxException {
        JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
        DynamicTextureJson out = CODEC.parse(JsonOps.INSTANCE, jsonObject).getOrThrow(false,s->{});
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
}
