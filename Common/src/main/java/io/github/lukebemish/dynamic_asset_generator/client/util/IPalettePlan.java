package io.github.lukebemish.dynamic_asset_generator.client.util;

import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.api.ResettingSupplier;
import io.github.lukebemish.dynamic_asset_generator.client.palette.Palette;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

public interface IPalettePlan {
    NativeImage getBackground() throws IOException;
    NativeImage getOverlay() throws IOException;
    NativeImage getPaletted() throws IOException;
    boolean includeBackground();
    boolean stretchPaletted();
    int extend();

    default ResettingSupplier<InputStream> getStream(ResourceLocation rl) {
        return ResettingSupplier.of(() -> {
            try (NativeImage image = Palette.paletteCombinedImage(this)) {
                if (image != null) {
                    return (InputStream) new ByteArrayInputStream(image.asByteArray());
                }
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Could not write buffered image to stream: {}...\n",rl, e);
            }
            return null;
        },this);
    }
    static ResettingSupplier<InputStream> supply(ResourceLocation rl, Supplier<IPalettePlan> plan_sup) {
        return ResettingSupplier.of(() -> {
            IPalettePlan planned = plan_sup.get();
            try (NativeImage image = Palette.paletteCombinedImage(planned)) {
                if (image != null) {
                    return (InputStream) new ByteArrayInputStream(image.asByteArray());
                }
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Could not write buffered image to stream: {}...\n",rl, e);
            }
            return null;
        },plan_sup);
    }
}
