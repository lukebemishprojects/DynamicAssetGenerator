package io.github.lukebemish.dynamic_asset_generator.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface IPathAwareInputStreamSource extends IInputStreamSource {


    @NotNull
    Set<ResourceLocation> getLocations();
}
