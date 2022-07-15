package io.github.lukebemish.dynamic_asset_generator.impl.tags;

import com.mojang.datafixers.util.Pair;
import io.github.lukebemish.dynamic_asset_generator.api.IPathAwareInputStreamSource;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class TagBuilder implements IPathAwareInputStreamSource {
    private final List<Supplier<Set<ResourceLocation>>> paths = new ArrayList<>();
    private final ResourceLocation location;

    public TagBuilder(ResourceLocation location) {
        this.location = location;
    }

    public void add(Pair<ResourceLocation,Supplier<Boolean>> p) {
        paths.add(() -> Boolean.TRUE.equals(p.getSecond().get())?Set.of(p.getFirst()):Set.of());
    }

    public void add(Supplier<Set<ResourceLocation>> rls) {
        paths.add(rls);
    }

    @Override
    public @NotNull Supplier<InputStream> get(ResourceLocation outRl) {
        return this::build;
    }

    private InputStream build() {
        StringBuilder internal = new StringBuilder();
        for (Supplier<Set<ResourceLocation>> p : paths) {
            p.get().forEach(rl -> {
                if (internal.length() >= 1) {
                    internal.append(",");
                }
                internal.append("\"").append(rl.getNamespace()).append(":").append(rl.getPath()).append("\"");
            });
        }
        String json = "{\"replace\":false,\"values\":["+internal+"]}";
        return new ByteArrayInputStream(json.getBytes());
    }

    @Override
    public Set<ResourceLocation> getLocations() {
        return Set.of(location);
    }
}
