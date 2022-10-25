package io.github.lukebemish.dynamic_asset_generator.api;

import com.mojang.datafixers.util.Pair;
import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.impl.ModConfig;
import io.github.lukebemish.dynamic_asset_generator.impl.client.JsonResourceGeneratorReader;
import io.github.lukebemish.dynamic_asset_generator.impl.tags.TagBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public class DataResourceCache extends ResourceCache {
    public static final DataResourceCache INSTANCE = new DataResourceCache();

    private final Map<ResourceLocation, TagBuilder> tagMap = new HashMap<>();
    private final List<Supplier<Map<ResourceLocation,Set<ResourceLocation>>>> tagQueue = new ArrayList<>();

    private DataResourceCache() {
        this.planSource(tagMap::keySet, rl -> tagMap.get(rl).get(rl));
        this.planSource(() -> new JsonResourceGeneratorReader(getSourceJsons()));
    }

    static Map<ResourceLocation, String> getSourceJsons() {
        HashMap<ResourceLocation, String> rls = new HashMap<>();
        HashSet<ResourceLocation> available = new HashSet<>();

        for (PackResources r : ServerPrePackRepository.getResources()) {
            if (r.getName().equals(DynamicAssetGenerator.CLIENT_PACK) || r.getName().equals(DynamicAssetGenerator.SERVER_PACK)) continue;
            for (String namespace : r.getNamespaces(PackType.SERVER_DATA)) {
                for (ResourceLocation rl : r.getResources(PackType.SERVER_DATA, namespace, SOURCE_JSON_DIR, x->x.toString().endsWith(".json"))) {
                    if (r.hasResource(PackType.SERVER_DATA,rl)) available.add(rl);
                }
            }
        }

        for (ResourceLocation rl : available) {
            try (InputStream resource = ServerPrePackRepository.getResource(rl)) {
                String text = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
                rls.put(rl, text);
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Issues loading resource source jsons...");
            }
        }
        return rls;
    }

    @Override
    public boolean shouldCache() {
        return DynamicAssetGenerator.getConfig().cacheData();
    }

    @Override
    public Path cachePath() {
        return ModConfig.DATA_CACHE_FOLDER;
    }


    @SuppressWarnings("unused")
    public void planTag(ResourceLocation tag, Pair<ResourceLocation, Supplier<Boolean>> p) {
        ResourceLocation rl = new ResourceLocation(tag.getNamespace(), "tags/"+tag.getPath()+".json");
        TagBuilder builder = tagMap.computeIfAbsent(rl, r->new TagBuilder(r,tagQueue));
        builder.add(p);
    }

    @SuppressWarnings("unused")
    public void planTag(ResourceLocation tag, Supplier<Set<ResourceLocation>> p) {
        ResourceLocation rl = new ResourceLocation(tag.getNamespace(), "tags/"+tag.getPath()+".json");
        TagBuilder builder = tagMap.computeIfAbsent(rl, r->new TagBuilder(r,tagQueue));
        builder.add(p);
    }

    @SuppressWarnings("unused")
    public void queueTags(Supplier<Map<ResourceLocation,Set<ResourceLocation>>> toQueue) {
        tagQueue.add(toQueue);
    }
}
