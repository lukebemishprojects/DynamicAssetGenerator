package io.github.lukebemish.dynamic_asset_generator.api.client;

import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.ModConfig;
import io.github.lukebemish.dynamic_asset_generator.api.ResourceCache;
import io.github.lukebemish.dynamic_asset_generator.client.JsonTextureSourceProviderSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class AssetResourceCache extends ResourceCache {
    private static final String SOURCE_JSON_DIR = "dynamic_assets_sources";
    public static final AssetResourceCache INSTANCE = new AssetResourceCache();

    private AssetResourceCache() {
        planSource(() -> new JsonTextureSourceProviderSource(getSourceJsons()));
    }

    static Map<ResourceLocation, String> getSourceJsons() {
        HashMap<ResourceLocation, String> rls = new HashMap<>();
        HashSet<ResourceLocation> available = new HashSet<>();

        for (PackResources r : ClientPrePackRepository.getResources()) {
            if (r.getName().equals(DynamicAssetGenerator.CLIENT_PACK) || r.getName().equals(DynamicAssetGenerator.SERVER_PACK)) continue;
            for (String namespace : r.getNamespaces(PackType.CLIENT_RESOURCES)) {
                for (ResourceLocation rl : r.getResources(PackType.CLIENT_RESOURCES, namespace, SOURCE_JSON_DIR, x->x.toString().endsWith(".json"))) {
                    if (r.hasResource(PackType.CLIENT_RESOURCES,rl)) available.add(rl);
                }
            }
        }

        for (ResourceLocation rl : available) {
            try (InputStream resource = ClientPrePackRepository.getResource(rl)) {
                String text = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
                rls.put(rl, text);
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Issues loading texture source jsons...");
            }
        }
        return rls;
    }

    @Override
    public boolean shouldCache() {
        return DynamicAssetGenerator.getConfig().cacheAssets();
    }

    @Override
    public Path cachePath() {
        return ModConfig.ASSET_CACHE_FOLDER;
    }
}
