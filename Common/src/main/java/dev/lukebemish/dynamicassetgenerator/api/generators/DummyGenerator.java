/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.generators;

import com.mojang.serialization.Codec;
import dev.lukebemish.dynamicassetgenerator.api.IResourceGenerator;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Set;

public class DummyGenerator implements IResourceGenerator {
    public static final DummyGenerator INSTANCE = new DummyGenerator();
    public static final Codec<DummyGenerator> CODEC = Codec.unit(INSTANCE);

    private DummyGenerator() {}

    @Override
    public IoSupplier<InputStream> get(ResourceLocation outRl, ResourceGenerationContext context) {
        return null;
    }

    @Override
    public @NotNull Set<ResourceLocation> getLocations() {
        return Set.of();
    }

    @Override
    public Codec<? extends IResourceGenerator> codec() {
        return CODEC;
    }
}
