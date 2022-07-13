package io.github.lukebemish.dynamic_asset_generator.api.client;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.function.Function;
import java.util.function.Supplier;

public interface ITexSource {
    Codec<ITexSource> TEXSOURCE_CODEC = ExtraCodecs.lazyInitializedCodec(() -> new Codec<Codec<? extends ITexSource>>() {
        @Override
        public <T> DataResult<Pair<Codec<? extends ITexSource>, T>> decode(DynamicOps<T> ops, T input) {
            return ResourceLocation.CODEC.decode(ops, input).flatMap(keyValuePair -> !GeneratedTextureHolder.SOURCES.containsKey(keyValuePair.getFirst())
                    ? DataResult.error("Unknown dynamic texture source type: " + keyValuePair.getFirst())
                    : DataResult.success(keyValuePair.mapFirst(GeneratedTextureHolder.SOURCES::get)));
        }

        @Override
        public <T> DataResult<T> encode(Codec<? extends ITexSource> input, DynamicOps<T> ops, T prefix) {
            ResourceLocation key = GeneratedTextureHolder.SOURCES.inverse().get(input);
            if (key == null)
            {
                return DataResult.error("Unregistered dynamic texture source type: " + input);
            }
            T toMerge = ops.createString(key.toString());
            return ops.mergeToPrimitive(prefix, toMerge);
        }
    }).dispatch(ITexSource::codec, Function.identity());
    Codec<? extends ITexSource> codec();
    Supplier<NativeImage> getSupplier() throws JsonSyntaxException;
}
