/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * An {@link InputStreamSource} that is aware of the locations it can provide input streams for.
 */
public interface PathAwareInputStreamSource extends InputStreamSource {

    /**
     * @return the locations that this {@link InputStreamSource} can provide resources at.
     */
    @NotNull
    Set<ResourceLocation> getLocations();
}
