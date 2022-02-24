package dynamic_asset_generator.client.api.json;

import com.google.gson.JsonSyntaxException;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

public interface ITexSource {
    Supplier<BufferedImage> getSupplier(String inputStr) throws JsonSyntaxException;
}
