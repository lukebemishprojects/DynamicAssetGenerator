package dynamic_asset_generator.client.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import dynamic_asset_generator.client.api.json.ITexSource;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

public class ColorSource implements ITexSource {
    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Override
    public Supplier<BufferedImage> getSupplier(String inputStr) throws JsonSyntaxException {
        LocationSource locationSource = gson.fromJson(inputStr, LocationSource.class);
        return () -> {
            BufferedImage out = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
            out.setRGB(0,0,locationSource.color);
            return out;
        };
    }

    public static class LocationSource {
        @Expose
        String source_type;
        @Expose
        public int color;
    }
}