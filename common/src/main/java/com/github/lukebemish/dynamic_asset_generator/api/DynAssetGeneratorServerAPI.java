package com.github.lukebemish.dynamic_asset_generator.api;

import com.github.lukebemish.dynamic_asset_generator.DynAssetGenServerPlanner;
import com.github.lukebemish.dynamic_asset_generator.Pair;
import com.github.lukebemish.dynamic_asset_generator.tags.TagBuilder;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DynAssetGeneratorServerAPI {
    private static final HashMap<ResourceLocation, TagBuilder> tagBuilderMap = new HashMap<>();
    public static void planLoadingStream(ResourceLocation location, Supplier<InputStream> sup) {
        DynAssetGenServerPlanner.planLoadingStream(location, sup);
    }
    public static void planLoaders(Supplier<Map<ResourceLocation,Supplier<InputStream>>> suppliers) {
        DynAssetGenServerPlanner.planLoaders(suppliers);
    }

    public static void planTagFile(ResourceLocation tagLocation, ResourceLocation toAdd) {
        planTagFileConditional(tagLocation, List.of(new Pair<>(toAdd,()->true)));
    }
    public static void planTagFile(ResourceLocation tagLocation, Collection<ResourceLocation> toAdd) {
        planTagFileConditional(tagLocation, toAdd.stream().map(rl->new Pair<ResourceLocation,Supplier<Boolean>>(rl,()->true)).toList());
    }
    public static void planTagFileConditional(ResourceLocation tagLocation, Pair<ResourceLocation,Supplier<Boolean>> toAdd) {
        planTagFileConditional(tagLocation, List.of(toAdd));
    }
    public static void planTagFileConditional(ResourceLocation tagLocation, Collection<Pair<ResourceLocation,Supplier<Boolean>>> toAdd) {
        if (!tagBuilderMap.containsKey(tagLocation)) {
            TagBuilder builder = new TagBuilder();
            tagBuilderMap.put(tagLocation,builder);
            planLoadingStream(new ResourceLocation(tagLocation.getNamespace(),"tags/"+tagLocation.getPath()+".json"),builder.supply());
        }
        for (Pair<ResourceLocation,Supplier<Boolean>> p : toAdd) {
            tagBuilderMap.get(tagLocation).add(p);
        }
    }
}
