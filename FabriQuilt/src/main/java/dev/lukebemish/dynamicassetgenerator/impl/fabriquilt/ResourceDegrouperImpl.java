/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.fabriquilt;

import com.google.auto.service.AutoService;
import net.minecraft.server.packs.PackResources;

import java.util.List;

@AutoService(dev.lukebemish.dynamicassetgenerator.impl.platform.services.ResourceDegrouper.class)
public class ResourceDegrouperImpl implements dev.lukebemish.dynamicassetgenerator.impl.platform.services.ResourceDegrouper {
    public List<? extends PackResources> unpackPacks(List<? extends PackResources> packs) {
        return FabriQuiltShared.getInstance().unpackPacks(packs);
    }
}
