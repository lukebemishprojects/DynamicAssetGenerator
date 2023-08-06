package dev.lukebemish.dynamicassetgenerator.impl;

import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class EmptyResourceSource implements ResourceGenerationContext.ResourceSource {
    public static final EmptyResourceSource INSTANCE = new EmptyResourceSource();

    private EmptyResourceSource() {}

    @Override
    public @Nullable IoSupplier<InputStream> getResource(@NotNull ResourceLocation location) {
        return null;
    }

    @Override
    public List<IoSupplier<InputStream>> getResourceStack(@NotNull ResourceLocation location) {
        return List.of();
    }

    @Override
    public Map<ResourceLocation, IoSupplier<InputStream>> listResources(@NotNull String namespace, @NotNull Predicate<ResourceLocation> filter) {
        return Map.of();
    }

    @Override
    public Map<ResourceLocation, List<IoSupplier<InputStream>>> listResourceStacks(@NotNull String namespace, @NotNull Predicate<ResourceLocation> filter) {
        return Map.of();
    }

    @Override
    public @NotNull Set<String> getNamespaces() {
        return Set.of();
    }
}
