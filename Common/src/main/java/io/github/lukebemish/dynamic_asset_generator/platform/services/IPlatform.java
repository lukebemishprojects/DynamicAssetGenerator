package io.github.lukebemish.dynamic_asset_generator.platform.services;

import java.nio.file.Path;

public interface IPlatform {
    Path getConfigFolder();
    boolean isDev();
}
