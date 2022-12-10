/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.templates;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.tags.TagEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

@SuppressWarnings("unused")
public record TagFile(List<TagEntry> values, boolean replace) {
    public static final Codec<TagFile> CODEC = RecordCodecBuilder.create(p-> p.group(
            TagEntry.CODEC.listOf().optionalFieldOf("values", List.of()).forGetter(TagFile::values),
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(TagFile::replace)
    ).apply(p, TagFile::new));

    static DataResult<TagFile> fromStream(InputStream stream) {
        try (var reader = new BufferedReader(new InputStreamReader(stream))) {
            JsonElement json = JsonParser.parseReader(reader);
            return CODEC.parse(JsonOps.INSTANCE, json);
        } catch (IOException | RuntimeException e) {
            return DataResult.error("Error reading tag file: " + e.getMessage());
        }
    }
}
