/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.platform.services;

import java.nio.file.Path;

public interface IPlatform {
    Path getConfigFolder();
    Path getModDataFolder();
}
