package dynamic_asset_generator.client.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import dynamic_asset_generator.DynamicAssetGenerator;
import dynamic_asset_generator.client.api.json.DynamicTextureJson;
import dynamic_asset_generator.client.api.json.ITexSource;
import dynamic_asset_generator.client.palette.ColorHolder;
import dynamic_asset_generator.client.util.SafeImageExtraction;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

public class Mask implements ITexSource {
    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Override
    public Supplier<BufferedImage> getSupplier(String inputStr) throws JsonSyntaxException {
        LocationSource locationSource = gson.fromJson(inputStr, LocationSource.class);
        Supplier<BufferedImage> input = DynamicTextureJson.readSupplierFromSource(locationSource.input);
        Supplier<BufferedImage> mask = DynamicTextureJson.readSupplierFromSource(locationSource.mask);

        return () -> {
            if (input == null || mask == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...");
                return null;
            }
            BufferedImage inImg = input.get();
            BufferedImage maskImg = mask.get();
            if (maskImg == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", locationSource.mask.toString());
                return null;
            }
            if (inImg == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", locationSource.input.toString());
                return null;
            }
            int maxX = Math.max(inImg.getWidth(),maskImg.getWidth());
            int maxY = inImg.getWidth() > maskImg.getWidth() ? inImg.getHeight() : maskImg.getHeight();
            int mxs,mys,ixs,iys;
            if (maskImg.getWidth() / (maskImg.getHeight() * 1.0) <= maxX / (maxY * 1.0)) {
                mxs = maxX/maskImg.getWidth();
                mys = maxY/maskImg.getWidth();
            } else {
                mxs = maxX/maskImg.getHeight();
                mys = maxY/maskImg.getHeight();
            }
            if (inImg.getWidth() / (inImg.getHeight() * 1.0) <= maxX / (maxY * 1.0)) {
                ixs = inImg.getWidth()/maxX;
                iys = inImg.getWidth()/maxY;
            } else {
                ixs = inImg.getHeight()/maxX;
                iys = inImg.getHeight()/maxY;
            }
            BufferedImage out = new BufferedImage(maxX, maxY, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < maxX; x++) {
                for (int y = 0; y < maxY; y++) {
                    ColorHolder mC = ColorHolder.fromColorInt(SafeImageExtraction.get(maskImg,x/mxs,y/mys));
                    ColorHolder iC = ColorHolder.fromColorInt(SafeImageExtraction.get(inImg,x/ixs,y/iys));
                    ColorHolder o = iC.withA(mC.getA() * iC.getA());
                    out.setRGB(x,y,ColorHolder.toColorInt(o));
                }
            }
            return out;
        };
    }

    public static class LocationSource {
        @Expose
        String source_type;
        @Expose
        public JsonObject input;
        @Expose
        public JsonObject mask;
    }
}
