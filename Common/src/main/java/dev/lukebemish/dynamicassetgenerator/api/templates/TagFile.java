/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.templates;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unused")
public record TagFile(List<Value> values, boolean replace) {
    public static final Codec<TagFile> CODEC = RecordCodecBuilder.create(p-> p.group(
            Value.CODEC.listOf().optionalFieldOf("values", List.of()).forGetter(TagFile::values),
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(TagFile::replace)
    ).apply(p, TagFile::new));

    public sealed interface Value {
        Codec<Value> CODEC = Codec.either(DirectReference.CODEC, TagReference.CODEC).xmap(
                either -> either.map(
                        directReference -> (Value) directReference,
                        tagReference -> tagReference
                ),
                value -> value instanceof DirectReference directReference
                        ? Either.left(directReference)
                        : Either.right((TagReference) value)
        );
    }

    public record DirectReference(ResourceLocation id, boolean required) implements Value {
        public static final Codec<DirectReference> CODEC = Codec.either(
                ResourceLocation.CODEC.xmap(rl -> new DirectReference(rl, true), DirectReference::id),
                RecordCodecBuilder.<DirectReference>create(p->p.group(
                        ResourceLocation.CODEC.fieldOf("id").forGetter(DirectReference::id),
                        Codec.BOOL.optionalFieldOf("required", true).forGetter(DirectReference::required)).apply(p, DirectReference::new)))
                .xmap(either ->
                        either.map(Function.identity(),Function.identity()), Either::right);
    }

    public record TagReference(ResourceLocation id) implements Value {
        public static final Codec<TagReference> CODEC = Codec.STRING.flatXmap(s->{
            if (!s.startsWith("#"))
                return DataResult.error("Tag must start with '#'");
            var location = ResourceLocation.tryParse(s.substring(1));
            if (location == null)
                return DataResult.error("Invalid tag location");
            return DataResult.success(location);
        },rl-> DataResult.success("#"+rl)).xmap(TagReference::new, TagReference::id);
    }

    static DataResult<TagFile> fromStream(InputStream stream) {
        try (var reader = new BufferedReader(new InputStreamReader(stream))) {
            JsonElement json = JsonParser.parseReader(reader);
            return CODEC.parse(JsonOps.INSTANCE, json);
        } catch (IOException | RuntimeException e) {
            return DataResult.error("Error reading tag file: " + e.getMessage());
        }
    }
}
