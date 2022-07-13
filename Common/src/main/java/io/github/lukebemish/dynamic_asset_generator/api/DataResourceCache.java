package io.github.lukebemish.dynamic_asset_generator.api;

import com.mojang.datafixers.util.Pair;
import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.ModConfig;
import io.github.lukebemish.dynamic_asset_generator.tags.TagBuilder;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DataResourceCache extends ResourceCache {
    public static final DataResourceCache INSTANCE = new DataResourceCache();

    private final Map<ResourceLocation, TagBuilder> tagMap = new HashMap<>();
    private DataResourceCache() {
        this.planSource(tagMap::keySet, rl -> tagMap.get(rl).get(rl));
    }

    @Override
    public boolean shouldCache() {
        return DynamicAssetGenerator.getConfig().cacheData();
    }

    @Override
    public Path cachePath() {
        return ModConfig.DATA_CACHE_FOLDER;
    }


    public void planTag(ResourceLocation tag, Pair<ResourceLocation, Supplier<Boolean>> p) {
        TagBuilder builder = tagMap.computeIfAbsent(tag, TagBuilder::new);
        builder.add(p);
    }
}
