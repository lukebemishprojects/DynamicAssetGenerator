package io.github.lukebemish.dynamic_asset_generator.client.util;

import io.github.lukebemish.dynamic_asset_generator.api.client.ClientPrePackRepository;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.lukebemish.dynamic_asset_generator.util.SupplierWithException;
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
