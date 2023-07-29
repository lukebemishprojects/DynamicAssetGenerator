package dev.lukebemish.dynamicassetgenerator.impl.client.platform;

import dev.lukebemish.dynamicassetgenerator.impl.platform.Services;

public class ClientServices {
    public static final PlatformClient PLATFORM_CLIENT = Services.load(PlatformClient.class);
}
