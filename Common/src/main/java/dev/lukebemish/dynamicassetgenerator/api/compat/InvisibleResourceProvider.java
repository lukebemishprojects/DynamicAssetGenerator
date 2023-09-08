/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.NonNull;

import java.io.InputStream;
import java.util.Set;

/**
 * A service used to provide resources that, for one reason or another, are not visible through the normal
 * PrePackRepositories. Examples would be adding compatibility with mods that have their own strange resource injection
 * systems. Should be provided as a service if this should always be available; otherwise, see
 * {@link ConditionalInvisibleResourceProvider}.
 */
public interface InvisibleResourceProvider {

    IoSupplier<InputStream> getResource(@NonNull PackType type, @NonNull ResourceLocation location);

    void listResources(@NonNull PackType type, @NonNull String namespace, @NonNull String path, PackResources.@NonNull ResourceOutput resourceOutput);

    Set<String> getNamespaces(@NonNull PackType type);

    default void reset(@NonNull PackType type) {}

}
