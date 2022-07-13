package io.github.lukebemish.dynamic_asset_generator.forge;

import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.client.DynamicAssetGeneratorClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DynamicAssetGenerator.MOD_ID)
public class DynamicAssetGeneratorForge {
    public DynamicAssetGeneratorForge() {
        IEventBus modbus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> DynamicAssetGeneratorClient::init);
        modbus.addListener(EventHandler::addResourcePack);
    }
}
