package io.github.lukebemish.dynamic_asset_generator.mixin;

import io.github.lukebemish.dynamic_asset_generator.api.ServerPrePackRepository;
import io.github.lukebemish.dynamic_asset_generator.api.client.ClientPrePackRepository;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin {
    @ModifyVariable(method = "<init>", at = @At(value = "HEAD"), argsOnly = true)
    private static List<PackResources> dynamic_asset_generator$loadPacks(List<PackResources> packs, PackType type, List<PackResources> packsAgain) {
        if (type == PackType.CLIENT_RESOURCES) {
            ClientPrePackRepository.resetResources();
        }
        if (type == PackType.SERVER_DATA) {
            ServerPrePackRepository.loadResources(packs);
        }
        return packs;
    }
}
