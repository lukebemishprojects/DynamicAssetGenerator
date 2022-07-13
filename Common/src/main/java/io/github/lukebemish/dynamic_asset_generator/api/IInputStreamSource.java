package io.github.lukebemish.dynamic_asset_generator.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.function.Supplier;

public interface IInputStreamSource {
    @NotNull
    Supplier<InputStream> get(ResourceLocation outRl);
}
