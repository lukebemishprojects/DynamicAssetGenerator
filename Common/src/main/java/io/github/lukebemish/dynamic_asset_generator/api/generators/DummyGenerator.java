package io.github.lukebemish.dynamic_asset_generator.api.generators;

import com.mojang.serialization.Codec;
import io.github.lukebemish.dynamic_asset_generator.api.IResourceGenerator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Set;
import java.util.function.Supplier;

public class DummyGenerator implements IResourceGenerator {
    public static final DummyGenerator INSTANCE = new DummyGenerator();
    public static final Codec<DummyGenerator> CODEC = Codec.unit(INSTANCE);

    private DummyGenerator() {}

    @Override
    public @NotNull Supplier<InputStream> get(ResourceLocation outRl) {
        return ()->null;
    }

    @Override
    public @NotNull Set<ResourceLocation> getLocations() {
        return Set.of();
    }

    @Override
    public Codec<? extends IResourceGenerator> codec() {
        return CODEC;
    }
}
