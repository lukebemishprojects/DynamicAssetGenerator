package dynamic_asset_generator.mixin;

import dynamic_asset_generator.DynamicAssetGenerator;
import dynamic_asset_generator.client.DynAssetGenClientResourcePack;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.SimpleReloadableResourceManager;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(SimpleReloadableResourceManager.class)
public abstract class SimpleReloadableResourceManagerMixin {

    @Inject(method = "createReload", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    private void dynamic_asset_generator_insertResourcePack(Executor preparationExecutor,
                                                            Executor reloadExecutor,
                                                            CompletableFuture<Unit> afterPreparation,
                                                            List<PackResources> packs,
                                                            CallbackInfoReturnable<ReloadInstance> cir) {
        if (type == PackType.CLIENT_RESOURCES) {
            DynamicAssetGenerator.LOGGER.info("Registering assets...");
            add(new DynAssetGenClientResourcePack());
        }
    }

    @Shadow
    public abstract void add(PackResources pack);

    @Shadow
    @Final
    private PackType type;
}
