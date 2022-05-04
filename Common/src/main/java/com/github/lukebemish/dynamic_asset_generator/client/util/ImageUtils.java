package com.github.lukebemish.dynamic_asset_generator.client.util;

import com.github.lukebemish.dynamic_asset_generator.client.api.ClientPrePackRepository;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

public class ImageUtils {
    public static NativeImage getImage(ResourceLocation location) throws IOException {
        return NativeImage.read(ClientPrePackRepository.getResource(location));
    }

    public static class ImageGetter implements SupplierWithException<NativeImage,IOException> {
        public ImageGetter(ResourceLocation rl) {this.rl = rl;}
        public final ResourceLocation rl;

        @Override
        public NativeImage get() throws IOException {
            return getImage(rl);
        }
    }
}
