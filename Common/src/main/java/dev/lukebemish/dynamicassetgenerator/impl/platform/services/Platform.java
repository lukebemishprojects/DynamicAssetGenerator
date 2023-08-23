/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.platform.services;

import java.nio.file.Path;

public interface Platform {
    Path getConfigFolder();
    Path getModDataFolder();
    String getModVersion();
}
