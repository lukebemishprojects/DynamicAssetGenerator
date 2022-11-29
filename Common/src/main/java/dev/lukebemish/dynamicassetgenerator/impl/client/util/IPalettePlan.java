/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client.util;

public interface IPalettePlan {
    boolean includeBackground();
    boolean stretchPaletted();
    int extend();
}
