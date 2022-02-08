package dynamic_asset_generator.mixin;

import dynamic_asset_generator.DynAssetGenServerDataPack;
import dynamic_asset_generator.api.ServerPrePackRepository;
import net.minecraft.server.ServerResources;
import net.minecraft.server.packs.PackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerResources.class)
public class ServerResourcesMixin {
    @ModifyArg(method = "loadResources", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/server/packs/resources/ReloadableResourceManager;reload(Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/List;Ljava/util/concurrent/CompletableFuture;)Ljava/util/concurrent/CompletableFuture;"),
            index = 2)
    private static List<PackResources> resourcePackList(List<PackResources> packs) {
        ServerPrePackRepository.loadResources(packs);
        ArrayList<PackResources> out = new ArrayList<>();
        out.addAll(packs);
        out.add(new DynAssetGenServerDataPack());
        return out;
    }
}
