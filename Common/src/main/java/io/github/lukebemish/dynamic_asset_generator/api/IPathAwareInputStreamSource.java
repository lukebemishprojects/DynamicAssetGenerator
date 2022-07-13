package io.github.lukebemish.dynamic_asset_generator.api;

import io.github.lukebemish.dynamic_asset_generator.api.IInputStreamSource;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public interface IPathAwareInputStreamSource extends IInputStreamSource {
    Set<ResourceLocation> location();
}
