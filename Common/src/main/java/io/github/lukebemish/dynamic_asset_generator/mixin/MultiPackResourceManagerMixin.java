package io.github.lukebemish.dynamic_asset_generator.mixin;

import io.github.lukebemish.dynamic_asset_generator.api.ServerPrePackRepository;
import io.github.lukebemish.dynamic_asset_generator.client.api.ClientPrePackRepository;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin {
    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void dynamic_asset_generator_insertResourcePack(PackType type,
                                                            List<PackResources> packs,
                                                            CallbackInfo cir) {
        if (type == PackType.CLIENT_RESOURCES) {
            ClientPrePackRepository.resetResources();
        }
        if (type == PackType.SERVER_DATA) {
            ServerPrePackRepository.loadResources(packs);
        }
    }
}
