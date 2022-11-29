/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.lukebemish.dynamicassetgenerator.impl.client.ClientRegisters;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

public interface ITexSource {
    Codec<ITexSource> CODEC = ExtraCodecs.lazyInitializedCodec(() -> new Codec<Codec<? extends ITexSource>>() {
        @Override
        public <T> DataResult<Pair<Codec<? extends ITexSource>, T>> decode(DynamicOps<T> ops, T input) {
            return ResourceLocation.CODEC.decode(ops, input).flatMap(keyValuePair -> !ClientRegisters.ITEXSOURCES.containsKey(keyValuePair.getFirst())
                    ? DataResult.error("Unknown dynamic texture source type: " + keyValuePair.getFirst())
                    : DataResult.success(keyValuePair.mapFirst(ClientRegisters.ITEXSOURCES::get)));
        }

        @Override
        public <T> DataResult<T> encode(Codec<? extends ITexSource> input, DynamicOps<T> ops, T prefix) {
            ResourceLocation key = ClientRegisters.ITEXSOURCES.inverse().get(input);
            if (key == null)
            {
                return DataResult.error("Unregistered dynamic texture source type: " + input);
            }
            T toMerge = ops.createString(key.toString());
            return ops.mergeToPrimitive(prefix, toMerge);
        }
    }).dispatch(ITexSource::codec, Function.identity());

    static void register(ResourceLocation rl, Codec<? extends ITexSource> reader) {
        ClientRegisters.ITEXSOURCES.put(rl, reader);
    }

    Codec<? extends ITexSource> codec();

    @NotNull
    Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException;
}
