/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl;

import dev.lukebemish.dynamicassetgenerator.impl.platform.Services;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Benchmarking {
    private static final boolean[] LOGGED = new boolean[2];

    public synchronized static void recordTime(String cache, ResourceLocation location, long time) {
        if (!Files.exists(Services.PLATFORM.getModDataFolder())) {
            try {
                Files.createDirectories(Services.PLATFORM.getModDataFolder());
            } catch (IOException e) {
                if (!LOGGED[0]) {
                    DynamicAssetGenerator.LOGGER.error("Issue creating mod data folder", e);
                    LOGGED[0] = true;
                }
                return;
            }
        }
        Path file = Services.PLATFORM.getModDataFolder().resolve("times.log");
        if (!Files.exists(file)) {
            try {
                Files.createFile(file);
            } catch (IOException e) {
                if (!LOGGED[1]) {
                    DynamicAssetGenerator.LOGGER.error("Issue writing to times.log", e);
                    LOGGED[1] = true;
                }
                return;
            }
        }
        try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            writer.write(cache + " " + location + " " + time + "\n");
        } catch (IOException e) {
            if (!LOGGED[1]) {
                DynamicAssetGenerator.LOGGER.error("Issue writing to times.log", e);
                LOGGED[1] = true;
            }
        }
    }
}
