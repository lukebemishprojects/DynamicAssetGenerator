package io.github.lukebemish.dynamic_asset_generator.quilt;

import io.github.lukebemish.dynamic_asset_generator.impl.client.DynAssetGenClientResourcePack;
import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.impl.client.DynamicAssetGeneratorClient;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;

public class DynamicAssetGeneratorClientQuilt implements ClientModInitializer {
    @Override
    public void onInitializeClient(ModContainer container) {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerResourcePackProfileProvider(((infoConsumer, infoFactory) -> {
            Pack pack = Pack.create(DynamicAssetGenerator.CLIENT_PACK, true, DynAssetGenClientResourcePack::new, infoFactory, Pack.Position.TOP, PackSource.DEFAULT);
            if (pack != null) {
                infoConsumer.accept(pack);
            } else {
                DynamicAssetGenerator.LOGGER.error("Couldn't inject client assets!");
            }
        }));
        DynamicAssetGeneratorClient.init();
    }
}
