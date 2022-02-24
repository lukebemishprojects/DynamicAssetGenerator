package dynamic_asset_generator.mixin;

import dynamic_asset_generator.api.ServerPrePackRepository;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ServerResources;
import net.minecraft.server.packs.PackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ServerResources.class)
public class ServerResourcesMixin {
    @Inject(method = "loadResources", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/packs/resources/ReloadableResourceManager;reload(Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/List;Ljava/util/concurrent/CompletableFuture;)Ljava/util/concurrent/CompletableFuture;", shift = At.Shift.AFTER))
    private static void dynamic_asset_generator_resourcePackList(List<PackResources> packs, RegistryAccess registryAccess, Commands.CommandSelection commandSelection, int i, Executor executor, Executor executor2, CallbackInfoReturnable<CompletableFuture<ServerResources>> ci) {
        ServerPrePackRepository.loadResources(packs);
    }
}
