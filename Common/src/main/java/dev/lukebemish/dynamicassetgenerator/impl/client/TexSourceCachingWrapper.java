/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record TexSourceCachingWrapper(ITexSource wrapped) implements ITexSource {
    @SuppressWarnings("unchecked")
    @Override
    public Codec<TexSourceCachingWrapper> codec() {
        if (unwrap(wrapped).codec() instanceof MapCodec.MapCodecCodec) {
            MapCodec.MapCodecCodec<? extends ITexSource> mapCodecCodec = (MapCodec.MapCodecCodec<? extends ITexSource>) unwrap(wrapped).codec();
            return mapCodecCodec.codec().xmap(TexSourceCachingWrapper::new, wrapper -> unsafeCast(unwrap(wrapper))).codec();
        }
        return unwrap(wrapped).codec().xmap(TexSourceCachingWrapper::new, wrapper -> unsafeCast(unwrap(wrapper)));
    }

    @SuppressWarnings("unchecked")
    private static <T extends ITexSource> T unsafeCast(ITexSource source) {
        return (T) source;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> wrapperImage = wrapped.getSupplier(data, context);
        if (wrapperImage == null) return null;
        return () -> TexSourceCache.fromCache(wrapperImage, unwrap(wrapped), context, data);
    }

    private static ITexSource unwrap(ITexSource source) {
        while (source instanceof TexSourceCachingWrapper wrapper) {
            source = wrapper.wrapped;
        }
        return source;
    }

    @Override
    public @NotNull <T> DataResult<T> cacheMetadata(DynamicOps<T> ops, TexSourceDataHolder data) {
        return wrapped.cacheMetadata(ops, data);
    }
}
