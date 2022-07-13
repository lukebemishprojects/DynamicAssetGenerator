package io.github.lukebemish.dynamic_asset_generator.api.client.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.api.client.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.client.util.ImageUtils;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.function.Supplier;

public record TextureReader(ResourceLocation path) implements ITexSource {
    public static final Codec<TextureReader> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("path").forGetter(TextureReader::path)
    ).apply(instance, TextureReader::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public Supplier<NativeImage> getSupplier() throws JsonSyntaxException {
        ResourceLocation outRl = new ResourceLocation(this.path().getNamespace(), "textures/"+this.path().getPath()+".png");
        return () -> {
            try {
                return ImageUtils.getImage(outRl);
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.debug("Issue loading texture: {}", this.path());
            }
            return null;
        };
    }
}
