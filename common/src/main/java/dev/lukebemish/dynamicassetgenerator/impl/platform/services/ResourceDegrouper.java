/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.platform.services;

import net.minecraft.server.packs.PackResources;

import java.util.stream.Stream;

public interface ResourceDegrouper {
    Stream<PackResources> unpackPacks(Stream<PackResources> packs);
}
