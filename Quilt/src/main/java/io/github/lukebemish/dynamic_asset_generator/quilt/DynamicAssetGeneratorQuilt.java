package io.github.lukebemish.dynamic_asset_generator.quilt;

import io.github.lukebemish.dynamic_asset_generator.DynAssetGenServerDataPack;
import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;

public class DynamicAssetGeneratorQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer container) {
        ResourceLoader.get(PackType.SERVER_DATA).registerResourcePackProfileProvider(((infoConsumer, infoFactory) -> {
            Pack pack = Pack.create(DynamicAssetGenerator.SERVER_PACK, true, DynAssetGenServerDataPack::new, infoFactory, Pack.Position.TOP, PackSource.DEFAULT);
            if (pack != null) {
                infoConsumer.accept(pack);
            } else {
                DynamicAssetGenerator.LOGGER.error("Couldn't inject server data!");
            }
        }));
    }
}
