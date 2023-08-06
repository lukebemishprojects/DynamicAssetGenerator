/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.forge;

import com.google.auto.service.AutoService;
import net.minecraft.server.packs.PackResources;
import net.minecraftforge.resource.DelegatingPackResources;

import java.util.ArrayList;
import java.util.List;

@AutoService(dev.lukebemish.dynamicassetgenerator.impl.platform.services.ResourceDegrouper.class)
public class ResourceDegrouperImpl implements dev.lukebemish.dynamicassetgenerator.impl.platform.services.ResourceDegrouper {
    public List<? extends PackResources> unpackPacks(List<? extends PackResources> packs) {
        ArrayList<PackResources> packsOut = new ArrayList<>();
        packs.forEach(pack -> {
            if (pack instanceof DelegatingPackResources delegatingResourcePack) {
                packsOut.addAll(delegatingResourcePack.getChildren());
            } else packsOut.add(pack);
        });
        return packsOut;
    }
}
