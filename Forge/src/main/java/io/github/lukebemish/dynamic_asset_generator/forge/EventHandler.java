package io.github.lukebemish.dynamic_asset_generator.forge;

import io.github.lukebemish.dynamic_asset_generator.client.DynAssetGenClientResourcePack;
import io.github.lukebemish.dynamic_asset_generator.DynAssetGenServerDataPack;
import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;

public class EventHandler {
    public static void addResourcePack(AddPackFindersEvent event) {
        DynamicAssetGenerator.LOGGER.info("Attempting pack insertion...");
        PackType type = event.getPackType();
        if (type == PackType.CLIENT_RESOURCES) {
            event.addRepositorySource((packConsumer, constructor) -> {
                Pack pack = Pack.create(DynamicAssetGenerator.CLIENT_PACK, true, DynAssetGenClientResourcePack::new, constructor, Pack.Position.TOP, PackSource.DEFAULT);
                if (pack != null) {
                    packConsumer.accept(pack);
                } else {
                    DynamicAssetGenerator.LOGGER.error("Couldn't inject client assets!");
                }
            });
        } else if (type == PackType.SERVER_DATA) {
            event.addRepositorySource((packConsumer, constructor) -> {
                Pack pack = Pack.create(DynamicAssetGenerator.SERVER_PACK, true, DynAssetGenServerDataPack::new, constructor, Pack.Position.TOP, PackSource.DEFAULT);
                if (pack != null) {
                    packConsumer.accept(pack);
                } else {
                    DynamicAssetGenerator.LOGGER.error("Couldn't inject server data!");
                }
            });
        }
    }
}
