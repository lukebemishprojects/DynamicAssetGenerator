package dynamic_asset_generator.mixin;

import dynamic_asset_generator.client.api.ClientPrePackRepository;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @ModifyVariable(method = {"reloadResourcePacks(Z)Ljava/util/concurrent/CompletableFuture;", "<init>"}, name = {"list"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;openAllSelected()Ljava/util/List;",shift = At.Shift.BY,by=2))
    private List<PackResources> dynamic_assets_modifyList(List<PackResources> resources) {
        ClientPrePackRepository.resetResources();
        return resources;
    }
}
