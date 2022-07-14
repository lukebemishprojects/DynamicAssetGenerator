package io.github.lukebemish.dynamic_asset_generator.api.client.generators;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class TexSourceDataHolder {
    public TexSourceDataHolder() {

    }

    public TexSourceDataHolder(TexSourceDataHolder old) {
        dataMap.putAll(old.dataMap);
    }

    private Map<String, Object> dataMap = new HashMap<>();

    public <T> void put(Class<? extends T> clazz, T data) {
        dataMap.put(clazz.descriptorString(), data);
    }

    @Nullable
    public <T> T get(Class<? extends T> clazz) {
        try {
            //noinspection unchecked
            return (T) dataMap.get(clazz.descriptorString());
        } catch (ClassCastException ignored) {
            return null;
        }
    }
}
