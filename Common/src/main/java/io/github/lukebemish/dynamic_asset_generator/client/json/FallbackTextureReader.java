package io.github.lukebemish.dynamic_asset_generator.client.json;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.client.api.json.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.client.util.ImageUtils;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.function.Supplier;

public class FallbackTextureReader implements ITexSource {
    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Override
    public Supplier<NativeImage> getSupplier(String inputStr) throws JsonSyntaxException{
        LocationSource locationSource = gson.fromJson(inputStr, LocationSource.class);
        ResourceLocation rl = ResourceLocation.of(locationSource.path,':');
        ResourceLocation out_rl = new ResourceLocation(rl.getNamespace(), "textures/"+rl.getPath()+".png");
        ResourceLocation fallback_rl = ResourceLocation.of(locationSource.fallback,':');
        ResourceLocation fallback_out_rl = new ResourceLocation(fallback_rl.getNamespace(), "textures/"+fallback_rl.getPath()+".png");
        return () -> {
            try {
                return ImageUtils.getImage(out_rl);
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.debug("Issue loading main texture: {}, trying fallback", rl);
                try {
                    return ImageUtils.getImage(fallback_out_rl);
                } catch (IOException e2) {
                    DynamicAssetGenerator.LOGGER.error("Issue loading main and fallback textures: {}, {}", rl, fallback_rl);
                }
            }
            return null;
        };
    }

    public static class LocationSource {
        @Expose
        String source_type;
        @Expose
        public String path;
        @Expose
        public String fallback;
    }
}
