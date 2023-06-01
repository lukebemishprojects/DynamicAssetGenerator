/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.platform.services;

import net.minecraft.server.packs.PackResources;

import java.util.List;

public interface ResourceDegrouper {
    List<? extends PackResources> unpackPacks(List<? extends PackResources> packs);
}
