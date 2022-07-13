package io.github.lukebemish.dynamic_asset_generator.api;

import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Supplier;

public abstract class ResourceCache {
    protected List<Supplier<? extends IPathAwareInputStreamSource>> cache = new ArrayList<>();

    public  Map<ResourceLocation, Supplier<InputStream>> getResources() {
        Map<ResourceLocation, Supplier<InputStream>> outputsSetup = new HashMap<>();
        this.cache.forEach(p-> {
            IPathAwareInputStreamSource source = p.get();
            Set<ResourceLocation> rls = source.location();
            rls.forEach(rl -> outputsSetup.put(rl, source.get(rl)));
        });

        var outputs = outputsSetup;

        if (shouldCache())
            outputs = wrapCachedData(outputs);

        return outputs;
    }

    private Map<ResourceLocation, Supplier<InputStream>> wrapCachedData(Map<ResourceLocation, Supplier<InputStream>> map) {
        HashMap<ResourceLocation, Supplier<InputStream>> output = new HashMap<>();
        map.forEach((rl, supplier) -> {
            Supplier<InputStream> wrapped = () -> {
                try {
                    Path path = this.cachePath().resolve(rl.getNamespace()).resolve(rl.getPath());
                    if (!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
                    if (!Files.exists(path)) {
                        InputStream stream = supplier.get();
                        if (stream != null) {
                            Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            return null;
                        }
                    }
                    return new BufferedInputStream(Files.newInputStream(path));
                } catch (IOException e) {
                    DynamicAssetGenerator.LOGGER.error("Could not cache resource...", e);
                }
                return null;
            };
            output.put(rl, wrapped);
        });
        return output;
    }

    public abstract boolean shouldCache();

    public abstract Path cachePath();

    public void planSource(ResourceLocation rl, IInputStreamSource source) {
        cache.add(wrap(()->Set.of(rl),source));
    }

    public void planSource(Supplier<Set<ResourceLocation>> locations, IInputStreamSource source) {
        cache.add(wrap(locations, source));
    }

    public void planSource(Set<ResourceLocation> locations, IInputStreamSource source) {
        cache.add(wrap(()->locations, source));
    }

    public void planSource(IPathAwareInputStreamSource source) {
        cache.add(()->source);
    }

    public void planSource(Supplier<? extends IPathAwareInputStreamSource> source) {
        cache.add(source);
    }

    public static Supplier<IPathAwareInputStreamSource> wrap(Supplier<Set<ResourceLocation>> rls, IInputStreamSource source) {
        return () -> new IPathAwareInputStreamSource() {
            @Override
            public Set<ResourceLocation> location() {
                return rls.get();
            }

            @Override
            public @NotNull Supplier<InputStream> get(ResourceLocation outRl) {
                return source.get(outRl);
            }
        };
    }
}
