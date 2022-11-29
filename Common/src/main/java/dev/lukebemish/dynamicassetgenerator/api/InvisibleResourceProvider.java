/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A service used to provide resources that, for one reason or another, are not visible through the normal
 * PrePackRepositories. Examples would be adding compatibility with mods that have their own strange resource injection
 * systems. Should be provided as a service if this should always be available; otherwise, see
 * {@link ConditionalInvisibleResourceProvider}.
 */
@ParametersAreNonnullByDefault
public interface InvisibleResourceProvider {

    InputStream getResource(PackType type, ResourceLocation location);

    Collection<ResourceLocation> getResources(PackType type, String namespace, String path, Predicate<ResourceLocation> filter);

    boolean hasResource(PackType type, ResourceLocation location);

    Set<String> getNamespaces(PackType type);

    default void reset(PackType type) {}

}
