/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.neoforge;

import com.google.auto.service.AutoService;
import dev.lukebemish.dynamicassetgenerator.impl.platform.services.ResourceDegrouper;
import net.minecraft.server.packs.PackResources;

import java.util.stream.Stream;

@AutoService(ResourceDegrouper.class)
public class ResourceDegrouperImpl implements ResourceDegrouper {
    public Stream<PackResources> unpackPacks(Stream<PackResources> packs) {
        // On modern neoforge, no flattening is needed!
        return packs;
    }
}
