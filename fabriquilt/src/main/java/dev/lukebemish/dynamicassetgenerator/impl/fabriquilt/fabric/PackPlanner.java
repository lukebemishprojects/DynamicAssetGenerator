/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.fabric;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.RepositorySource;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class PackPlanner {
    private static final Map<PackType, PackPlanner> PLANNERS = new IdentityHashMap<>();

    private final List<RepositorySource> sources = new ArrayList<>();

    public void register(RepositorySource consumer) {
        sources.add(consumer);
    }

    public static PackPlanner forType(PackType type) {
        return PLANNERS.computeIfAbsent(type, t -> new PackPlanner());
    }

    public List<Pack> plan() {
        List<Pack> packs = new ArrayList<>();
        for (RepositorySource source : sources) {
            source.loadPacks(packs::add);
        }
        return packs;
    }
}
