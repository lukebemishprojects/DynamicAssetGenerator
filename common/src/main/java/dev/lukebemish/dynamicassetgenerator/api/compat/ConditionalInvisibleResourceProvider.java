/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.compat;

/**
 * A service that conditionally provides an {@link InvisibleResourceProvider}. Useful if such a provider should not
 * always be present - for instance, if it should only be present if a certain mod is loaded. Using this class allows
 * classloading separation of the linked resource provider.
 */
public interface ConditionalInvisibleResourceProvider {
    boolean isAvailable();
    InvisibleResourceProvider get();
}
