package dev.lukebemish.dynamicassetgenerator.impl;

import com.google.common.base.Suppliers;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

@FunctionalInterface
public interface ResourceFinder {
    ResourceFinder[] INSTANCES = new ResourceFinder[2];

    List<PackResources> getPacks();


}
