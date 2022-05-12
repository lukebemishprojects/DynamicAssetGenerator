package io.github.lukebemish.dynamic_asset_generator.tags;

import io.github.lukebemish.dynamic_asset_generator.Pair;
import io.github.lukebemish.dynamic_asset_generator.api.ResettingSupplier;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TagBuilder {
    private final List<Pair<ResourceLocation, Supplier<Boolean>>> paths = new ArrayList<>();

    public void add(Pair<ResourceLocation,Supplier<Boolean>> p) {
        paths.add(p);
    }

    public ResettingSupplier<InputStream> supply() {
        return new ResettingSupplier<InputStream>() {
            @Override
            public void reset() {
                for (Pair<ResourceLocation,Supplier<Boolean>> p : paths) {
                    if (p.last() instanceof ResettingSupplier<Boolean> rs) {
                        rs.reset();
                    }
                }
            }

            @Override
            public InputStream get() {
                return build();
            }
        };
    }

    private InputStream build() {
        StringBuilder internal = new StringBuilder();
        for (Pair<ResourceLocation, Supplier<Boolean>> p : paths) {
            if (p.last().get()) {
                var rl = p.first();
                if (internal.length() >= 1) {
                    internal.append(",");
                }
                internal.append("\"").append(rl.getNamespace()).append(":").append(rl.getPath()).append("\"");
            }
        }
        String json = "{\"replace\":false,\"values\":["+internal+"]}";
        return new ByteArrayInputStream(json.getBytes());
    }
}
