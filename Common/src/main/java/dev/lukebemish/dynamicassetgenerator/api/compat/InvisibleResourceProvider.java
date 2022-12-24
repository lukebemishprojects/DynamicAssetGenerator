/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Set;

/**
 * A service used to provide resources that, for one reason or another, are not visible through the normal
 * PrePackRepositories. Examples would be adding compatibility with mods that have their own strange resource injection
 * systems. Should be provided as a service if this should always be available; otherwise, see
 * {@link ConditionalInvisibleResourceProvider}.
 */
public interface InvisibleResourceProvider {

    IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location);

    void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String path, @NotNull PackResources.ResourceOutput resourceOutput);

    Set<String> getNamespaces(@NotNull PackType type);

    default void reset(@NotNull PackType type) {}

}
