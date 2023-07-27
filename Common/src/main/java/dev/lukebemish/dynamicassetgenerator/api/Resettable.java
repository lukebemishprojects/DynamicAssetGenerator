/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api;

/**
 * A listener called when a resource cache is reset.
 */
@FunctionalInterface
public interface Resettable {
    /**
     * Resets some state associated with sources registered to the resource cache.
     */
    void reset();
}
