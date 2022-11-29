/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.function.Supplier;

@FunctionalInterface
public interface IInputStreamSource {
    @NotNull
    Supplier<InputStream> get(ResourceLocation outRl);
}
