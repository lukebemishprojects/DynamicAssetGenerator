package io.github.lukebemish.dynamic_asset_generator.fabric;

import com.google.auto.service.AutoService;
import io.github.lukebemish.dynamic_asset_generator.fabric.mixin.IGroupResourcePackMixin;
import io.github.lukebemish.dynamic_asset_generator.platform.services.IResourceDegrouper;
import net.fabricmc.fabric.impl.resource.loader.GroupResourcePack;
import net.minecraft.server.packs.PackResources;

import java.util.ArrayList;
import java.util.List;

@AutoService(IResourceDegrouper.class)
public class ResourceDegrouper implements IResourceDegrouper {
    public List<? extends PackResources> unpackPacks(List<? extends PackResources> packs) {
        if (packs.stream().noneMatch(pack->pack instanceof GroupResourcePack)) {
            return packs;
        }
        ArrayList<PackResources> outPacks = new ArrayList<>();
        for (var pack : packs) {
            if (pack instanceof GroupResourcePack groupResourcePack) {
                outPacks.addAll(unpackPacks(((IGroupResourcePackMixin)groupResourcePack).getPacks()));
            } else {
                outPacks.add(pack);
            }
        }
        return outPacks;
    }
}
