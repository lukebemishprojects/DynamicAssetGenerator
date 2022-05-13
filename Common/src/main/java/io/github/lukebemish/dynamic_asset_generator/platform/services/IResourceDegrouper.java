package io.github.lukebemish.dynamic_asset_generator.platform.services;

import net.minecraft.server.packs.PackResources;

import java.util.List;

public interface IResourceDegrouper {
    List<? extends PackResources> unpackPacks(List<? extends PackResources> packs);
}
