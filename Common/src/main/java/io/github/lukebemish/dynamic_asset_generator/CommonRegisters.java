package io.github.lukebemish.dynamic_asset_generator;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import io.github.lukebemish.dynamic_asset_generator.api.IResourceGenerator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class CommonRegisters {
    private CommonRegisters() {}
    public static final BiMap<ResourceLocation, Codec<? extends IResourceGenerator>> IRESOURCEGENERATORS = HashBiMap.create();
}
