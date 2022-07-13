package io.github.lukebemish.dynamic_asset_generator.client;

import io.github.lukebemish.dynamic_asset_generator.api.IPathAwareInputStreamSource;
import io.github.lukebemish.dynamic_asset_generator.api.client.GeneratedTextureHolder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class JsonTextureSourceProviderSource implements IPathAwareInputStreamSource {
    private final Map<ResourceLocation, GeneratedTextureHolder> map = new HashMap<>();
    public JsonTextureSourceProviderSource(Map<ResourceLocation, String> map) {
        map.forEach((rl, str) -> {
            GeneratedTextureHolder json = GeneratedTextureHolder.fromJson(str);
            if (json!=null)
                this.map.put(json.getOutputLocation(), json);
        } );
    }

    @Override
    public @NotNull Supplier<InputStream> get(ResourceLocation outRl) {
        GeneratedTextureHolder json = map.get(outRl);
        if (json!=null)
            return json.get(outRl);
        return ()->null;
    }

    @Override
    public Set<ResourceLocation> location() {
        return map.keySet();
    }
}
