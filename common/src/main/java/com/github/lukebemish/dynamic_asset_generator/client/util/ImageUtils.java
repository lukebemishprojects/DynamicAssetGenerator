package com.github.lukebemish.dynamic_asset_generator.client.util;

import com.github.lukebemish.dynamic_asset_generator.client.api.ClientPrePackRepository;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageUtils {
    public static BufferedImage getImage(ResourceLocation location) throws IOException {
        return ImageIO.read(ClientPrePackRepository.getResource(location));
    }

    public static class ImageGetter implements SupplierWithException<BufferedImage,IOException> {
        public ImageGetter(ResourceLocation rl) {this.rl = rl;}
        public final ResourceLocation rl;

        @Override
        public BufferedImage get() throws IOException {
            return getImage(rl);
        }
    }
}
