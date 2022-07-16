package io.github.lukebemish.dynamic_asset_generator.quilt;

import com.google.auto.service.AutoService;
import io.github.lukebemish.dynamic_asset_generator.impl.platform.services.IResourceDegrouper;
import net.minecraft.server.packs.PackResources;
import org.quiltmc.qsl.resource.loader.api.GroupResourcePack;

import java.util.ArrayList;
import java.util.List;

@AutoService(IResourceDegrouper.class)
public class ResourceDegrouper implements IResourceDegrouper {
    public List<? extends PackResources> unpackPacks(List<? extends PackResources> packs) {
        ArrayList<PackResources> packsOut = new ArrayList<>();
        packs.forEach(pack -> {
            if (pack instanceof GroupResourcePack groupResourcePack) {
                packsOut.addAll(groupResourcePack.getPacks());
            } else packsOut.add(pack);
        });
        return packsOut;
    }
}
