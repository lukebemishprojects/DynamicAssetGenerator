package dynamic_asset_generator.client.util;

import java.awt.image.BufferedImage;
import java.io.IOException;

public interface IPalettePlan {
    public BufferedImage getBackground() throws IOException;
    public BufferedImage getOverlay() throws IOException;
    public BufferedImage getPaletted() throws IOException;
    public boolean includeBackground();
    public boolean stretchPaletted();
    public int extend();
}
