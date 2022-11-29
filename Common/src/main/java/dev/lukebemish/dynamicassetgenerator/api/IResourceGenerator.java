/*
 * Copyright (C) 2022 Luke Bemish and contributors
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

public interface IResourceGenerator extends IPathAwareInputStreamSource {
    Codec<IResourceGenerator> CODEC = ExtraCodecs.lazyInitializedCodec(() -> new Codec<Codec<? extends IResourceGenerator>>() {
        @Override
        public <T> DataResult<Pair<Codec<? extends IResourceGenerator>, T>> decode(DynamicOps<T> ops, T input) {
            return ResourceLocation.CODEC.decode(ops, input).flatMap(keyValuePair -> !CommonRegisters.IRESOURCEGENERATORS.containsKey(keyValuePair.getFirst())
                    ? DataResult.error("Unknown dynamic resource generator type: " + keyValuePair.getFirst())
                    : DataResult.success(keyValuePair.mapFirst(CommonRegisters.IRESOURCEGENERATORS::get)));
        }

        @Override
        public <T> DataResult<T> encode(Codec<? extends IResourceGenerator> input, DynamicOps<T> ops, T prefix) {
            ResourceLocation key = CommonRegisters.IRESOURCEGENERATORS.inverse().get(input);
            if (key == null)
            {
                return DataResult.error("Unregistered dynamic resource generator type: " + input);
            }
            T toMerge = ops.createString(key.toString());
            return ops.mergeToPrimitive(prefix, toMerge);
        }
    }).dispatch(IResourceGenerator::codec, Function.identity());

    static void register(ResourceLocation rl, Codec<? extends IResourceGenerator> reader) {
        CommonRegisters.IRESOURCEGENERATORS.put(rl, reader);
    }


    Codec<? extends IResourceGenerator> codec();
}
