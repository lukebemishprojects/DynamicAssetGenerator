package io.github.lukebemish.dynamic_asset_generator.client.json;

import io.github.lukebemish.dynamic_asset_generator.client.NativeImageHelper;
import io.github.lukebemish.dynamic_asset_generator.client.api.json.ITexSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.mojang.blaze3d.platform.NativeImage;

import java.util.List;
import java.util.function.Supplier;

public class ColorSource implements ITexSource {
    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Override
    public Supplier<NativeImage> getSupplier(String inputStr) throws JsonSyntaxException {
        LocationSource lS = gson.fromJson(inputStr, LocationSource.class);
        return () -> {
            int len = Math.min(128*128,lS.color.size());
            int sideLength = 0;
            for (int i = 0; i < 8; i++) {
                sideLength = (int) Math.pow(2,i);
                if (Math.pow(2,i)*Math.pow(2,i)>=len) {
                    break;
                }
            }
            NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA,sideLength,sideLength,false);
            outer:
            for (int y = 0; y < sideLength; y++) {
                for (int x = 0; x < sideLength; x++) {
                    if (x+sideLength*y >= len) {
                        break outer;
                    }
                    out.setPixelRGBA(x,y,lS.color.get(x+sideLength*y));
                }
            }
            return out;
        };
    }

    public static class LocationSource {
        @Expose
        String source_type;
        @Expose
        public List<Integer> color;
    }
}