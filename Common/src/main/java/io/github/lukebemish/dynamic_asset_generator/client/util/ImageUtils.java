package io.github.lukebemish.dynamic_asset_generator.client.util;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.lukebemish.dynamic_asset_generator.api.client.ClientPrePackRepository;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

public class ImageUtils {
    public static NativeImage getImage(ResourceLocation location) throws IOException {
        return NativeImage.read(ClientPrePackRepository.getResource(location));
    }
}
