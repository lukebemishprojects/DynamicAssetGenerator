package io.github.lukebemish.dynamic_asset_generator.quilt;

import com.google.auto.service.AutoService;
import io.github.lukebemish.dynamic_asset_generator.platform.services.IResourceDegrouper;
import net.minecraft.server.packs.PackResources;

import java.util.List;

@AutoService(IResourceDegrouper.class)
public class ResourceDegrouper implements IResourceDegrouper {
    public List<? extends PackResources> unpackPacks(List<? extends PackResources> packs) {
        return packs;
    }
}
