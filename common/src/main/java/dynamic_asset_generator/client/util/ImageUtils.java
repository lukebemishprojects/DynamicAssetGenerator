package dynamic_asset_generator.client.util;

import dynamic_asset_generator.client.api.ClientPrePackRepository;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageUtils {
    public static BufferedImage getImage(ResourceLocation location) throws IOException {
        BufferedImage image = ImageIO.read(ClientPrePackRepository.getResource(location));
        return image;
    }
}
