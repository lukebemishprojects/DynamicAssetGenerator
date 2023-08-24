package dev.lukebemish.dynamicassetgenerator.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;

public class TrackingResourceSource implements ResourceGenerationContext.ResourceSource {

    private final ResourceGenerationContext.ResourceSource delegate;
    private final List<ResourceLocation> touchedTextures = new ArrayList<>();
    private final Set<ResourceLocation> touchedTexturesSet = new HashSet<>();
    private final List<ResourceLocation> view = Collections.unmodifiableList(touchedTextures);

    private final String prefix;
    private final String suffix;

    /**
     * @return the list of resources that have been touched by generators seeing this
     */
    public List<ResourceLocation> getTouchedTextures() {
        return view;
    }

    private TrackingResourceSource(ResourceGenerationContext.ResourceSource delegate, String prefix, String suffix) {
        this.delegate = delegate;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public static TrackingResourceSource of(ResourceGenerationContext.ResourceSource delegate, String prefix, String suffix) {
        return new TrackingResourceSource(delegate, prefix, suffix);
    }

    private void addLocation(ResourceLocation location) {
        if (location.getPath().startsWith(prefix) && location.getPath().endsWith(suffix)) {
            unsafeAddLocation(new ResourceLocation(location.getNamespace(), location.getPath().substring(prefix.length(), location.getPath().length() - suffix.length())));
        }
    }

    private synchronized void unsafeAddLocation(ResourceLocation location) {
        if (touchedTexturesSet.add(location)) {
            touchedTextures.add(location);
        }
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(@NotNull ResourceLocation location) {
        addLocation(location);
        return delegate.getResource(location);
    }

    @Override
    public List<IoSupplier<InputStream>> getResourceStack(@NotNull ResourceLocation location) {
        addLocation(location);
        return delegate.getResourceStack(location);
    }

    @Override
    public Map<ResourceLocation, IoSupplier<InputStream>> listResources(@NotNull String path, @NotNull Predicate<ResourceLocation> filter) {
        var resources = delegate.listResources(path, filter);
        resources.keySet().forEach(this::addLocation);
        return resources;
    }

    @Override
    public Map<ResourceLocation, List<IoSupplier<InputStream>>> listResourceStacks(@NotNull String path, @NotNull Predicate<ResourceLocation> filter) {
        var resources = delegate.listResourceStacks(path, filter);
        resources.keySet().forEach(this::addLocation);
        return resources;
    }

    @Override
    public @NotNull Set<String> getNamespaces() {
        return delegate.getNamespaces();
    }
}
