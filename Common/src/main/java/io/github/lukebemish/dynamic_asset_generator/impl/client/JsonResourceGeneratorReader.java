package io.github.lukebemish.dynamic_asset_generator.impl.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import io.github.lukebemish.dynamic_asset_generator.api.IPathAwareInputStreamSource;
import io.github.lukebemish.dynamic_asset_generator.api.IResourceGenerator;
import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class JsonResourceGeneratorReader implements IPathAwareInputStreamSource {
    private final Map<ResourceLocation, IResourceGenerator> map = new HashMap<>();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    public JsonResourceGeneratorReader(Map<ResourceLocation, String> map) {
        map.forEach((rl, str) -> {
            try {
                IResourceGenerator json = fromJson(str);
                if (json != null && json.location().size() > 0) {
                    json.location().forEach(localRl -> this.map.put(localRl, json));
                }
            } catch (RuntimeException e) {
                DynamicAssetGenerator.LOGGER.error("Could not read json source at {}\n",rl,e);
            }
        } );
    }

    @Nullable
    static IResourceGenerator fromJson(String json) throws JsonSyntaxException {
        JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
        return IResourceGenerator.CODEC.parse(JsonOps.INSTANCE, jsonObject).getOrThrow(false, s->{});
    }

    @Override
    public @NotNull Supplier<InputStream> get(ResourceLocation outRl) {
        IResourceGenerator json = map.get(outRl);
        if (json!=null)
            return json.get(outRl);
        return ()->null;
    }

    @Override
    public Set<ResourceLocation> location() {
        return map.keySet();
    }
}
