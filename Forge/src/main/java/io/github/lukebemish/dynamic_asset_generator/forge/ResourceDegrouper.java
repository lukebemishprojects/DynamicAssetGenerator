package io.github.lukebemish.dynamic_asset_generator.forge;

import com.google.auto.service.AutoService;
import io.github.lukebemish.dynamic_asset_generator.forge.mixin.DelegatingResourcePackAccessor;
import io.github.lukebemish.dynamic_asset_generator.platform.services.IResourceDegrouper;
import net.minecraft.server.packs.PackResources;
import net.minecraftforge.resource.DelegatingResourcePack;

import java.util.ArrayList;
import java.util.List;

@AutoService(IResourceDegrouper.class)
public class ResourceDegrouper implements IResourceDegrouper {
    public List<? extends PackResources> unpackPacks(List<? extends PackResources> packs) {
        ArrayList<PackResources> packsOut = new ArrayList<>();
        packs.forEach(pack -> {
            if (pack instanceof DelegatingResourcePack delegatingResourcePack) {
                packsOut.addAll(((DelegatingResourcePackAccessor)delegatingResourcePack).getDelegates());
            } else packsOut.add(pack);
        });
        return packsOut;
    }
}
