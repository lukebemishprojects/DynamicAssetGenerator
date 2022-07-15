package io.github.lukebemish.dynamic_asset_generator.impl.client;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.ITexSource;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ClientRegisters {
    private ClientRegisters() {}
    public static final BiMap<ResourceLocation, Codec<? extends ITexSource>> ITEXSOURCES = HashBiMap.create();
}
