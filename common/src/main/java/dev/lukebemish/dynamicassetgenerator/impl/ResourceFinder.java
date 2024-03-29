/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl;

import net.minecraft.server.packs.PackResources;

import java.util.stream.Stream;

@FunctionalInterface
public interface ResourceFinder {
    ResourceFinder[] INSTANCES = new ResourceFinder[2];

    Stream<PackResources> getPacks();
}
