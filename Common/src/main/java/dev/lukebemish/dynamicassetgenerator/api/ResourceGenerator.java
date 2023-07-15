/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.lukebemish.dynamicassetgenerator.impl.CommonRegisters;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.function.Function;

/**
 * Represents a set of instructions to generate any number of resources, to be read from JSON.
 */
public interface ResourceGenerator extends PathAwareInputStreamSource {
    Codec<ResourceGenerator> CODEC = ExtraCodecs.lazyInitializedCodec(() -> new Codec<Codec<? extends ResourceGenerator>>() {
        @Override
        public <T> DataResult<Pair<Codec<? extends ResourceGenerator>, T>> decode(DynamicOps<T> ops, T input) {
            return ResourceLocation.CODEC.decode(ops, input).flatMap(keyValuePair -> !CommonRegisters.RESOURCEGENERATORS.containsKey(keyValuePair.getFirst())
                    ? DataResult.error(() -> "Unknown dynamic resource generator type: " + keyValuePair.getFirst())
                    : DataResult.success(keyValuePair.mapFirst(CommonRegisters.RESOURCEGENERATORS::get)));
        }

        @Override
        public <T> DataResult<T> encode(Codec<? extends ResourceGenerator> input, DynamicOps<T> ops, T prefix) {
            ResourceLocation key = CommonRegisters.RESOURCEGENERATORS.inverse().get(input);
            if (key == null)
            {
                return DataResult.error(() -> "Unregistered dynamic resource generator type: " + input);
            }
            T toMerge = ops.createString(key.toString());
            return ops.mergeToPrimitive(prefix, toMerge);
        }
    }).dispatch(ResourceGenerator::codec, Function.identity());

    /**
     * Registers a new resource generator type.
     * @param rl The resource location to register the generator under; becomes the {@code "type"} field in JSON.
     * @param reader The codec used to deserialize the generator from JSON.
     */
    static void register(ResourceLocation rl, Codec<? extends ResourceGenerator> reader) {
        CommonRegisters.RESOURCEGENERATORS.put(rl, reader);
    }

    /**
     * @return A codec that can serialize this resource generator.
     */
    Codec<? extends ResourceGenerator> codec();
}
