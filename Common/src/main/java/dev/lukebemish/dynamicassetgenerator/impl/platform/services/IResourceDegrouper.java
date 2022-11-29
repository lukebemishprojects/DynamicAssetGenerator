/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.platform.services;

import net.minecraft.server.packs.PackResources;

import java.util.List;

public interface IResourceDegrouper {
    List<? extends PackResources> unpackPacks(List<? extends PackResources> packs);
}
