package dynamic_asset_generator.util;

import dynamic_asset_generator.api.PrePackRepository;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageUtils {
    public static BufferedImage getImage(ResourceLocation location) throws IOException {
        BufferedImage image = ImageIO.read(PrePackRepository.getResource(location));
        return image;
    }
}
