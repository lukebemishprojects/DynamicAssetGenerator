/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client.platform;

import dev.lukebemish.dynamicassetgenerator.impl.platform.Services;

public class ClientServices {
    public static final PlatformClient PLATFORM_CLIENT = Services.load(PlatformClient.class);
}
