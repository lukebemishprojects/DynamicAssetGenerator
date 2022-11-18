package io.github.lukebemish.dynamic_asset_generator.api;

import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
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
    protected static final String SOURCE_JSON_DIR = "dynamic_asset_generator";
    protected List<Supplier<? extends IPathAwareInputStreamSource>> cache = new ArrayList<>();
    private final List<Runnable> resetListeners = new ArrayList<>();

    public  Map<ResourceLocation, Supplier<InputStream>> getResources() {
        Map<ResourceLocation, Supplier<InputStream>> outputsSetup = new HashMap<>();
        this.cache.forEach(p-> {
            try {
                IPathAwareInputStreamSource source = p.get();
                Set<ResourceLocation> rls = source.getLocations();
                rls.forEach(rl -> outputsSetup.put(rl, wrapSafeData(rl, source.get(rl))));
            } catch (Throwable e) {
                DynamicAssetGenerator.LOGGER.error("Issue setting up IPathAwareInputStreamSource:",e);
            }
        });

        var outputs = outputsSetup;

        if (shouldCache())
            outputs = wrapCachedData(outputs);

        return outputs;
    }

    @SuppressWarnings("unused")
    public void planResetListener(Runnable listener) {
        this.resetListeners.add(listener);
    }

    public void reset() {
        this.resetListeners.forEach(Runnable::run);
    }

    private Supplier<InputStream> wrapSafeData(ResourceLocation rl, Supplier<InputStream> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Throwable e) {
                DynamicAssetGenerator.LOGGER.error("Issue reading supplying resource {}:", rl, e);
                return null;
            }
        };
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

    @SuppressWarnings("unused")
    public void planSource(ResourceLocation rl, IInputStreamSource source) {
        cache.add(wrap(()->Set.of(rl),source));
    }

    @SuppressWarnings("unused")
    public void planSource(Supplier<Set<ResourceLocation>> locations, IInputStreamSource source) {
        cache.add(wrap(locations, source));
    }

    @SuppressWarnings("unused")
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
            public @NotNull Set<ResourceLocation> getLocations() {
                return rls.get();
            }

            @Override
            public @NotNull Supplier<InputStream> get(ResourceLocation outRl) {
                return source.get(outRl);
            }
        };
    }
}
