package io.github.lukebemish.dynamic_asset_generator.api.client.generators.texsources.mask;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.TexSourceDataHolder;
import io.github.lukebemish.dynamic_asset_generator.impl.client.NativeImageHelper;
import io.github.lukebemish.dynamic_asset_generator.impl.client.palette.ColorHolder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record EdgeMask(ITexSource input, boolean countOutsideFrame, boolean countDiagonals, float cutoff) implements ITexSource {
    public static final Codec<EdgeMask> CODEC = RecordCodecBuilder.create(i -> i.group(
            ITexSource.CODEC.fieldOf("input").forGetter(EdgeMask::input),
            Codec.BOOL.optionalFieldOf("count_outside_frame",false).forGetter(EdgeMask::countOutsideFrame),
            Codec.BOOL.optionalFieldOf("count_diagonals",false).forGetter(EdgeMask::countOutsideFrame),
            Codec.FLOAT.optionalFieldOf("cutoff",0.5f).forGetter(EdgeMask::cutoff)
    ).apply(i, EdgeMask::new));

    private static final int[] COUNT_X = new int[] {-1, 1, 0, 0};
    private static final int[] COUNT_Y = new int[] {0, 0, -1, 1};
    private static final int[] DIAGONAL_X = new int[] {-1, 1, -1, 1};
    private static final int[] DIAGONAL_Y = new int[] {-1, 1, 1, -1};

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @NotNull Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException {
        Supplier<NativeImage> input = this.input.getSupplier(data);
        return () -> {
            try (NativeImage inImg = input.get()) {
                if (inImg == null) {
                    data.getLogger().error("Texture given was nonexistent...\n{}", this.input);
                    return null;
                }
                int width = inImg.getWidth();
                int height = inImg.getHeight();
                NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA, width, height, false);
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < width; y++) {
                        boolean isEdge = false;
                        if (countOutsideFrame && (x == 0 || y == 0 || x == width-1 || y == width-1))
                            isEdge = true;
                        else {
                            if (ColorHolder.fromColorInt(inImg.getPixelRGBA(x, y)).getA() > cutoff) {
                                for (int i = 0; i < 4; i++) {
                                    int x1 = COUNT_X[i]+x;
                                    int y1 = COUNT_Y[i]+y;
                                    if (ColorHolder.fromColorInt(inImg.getPixelRGBA(x1,y1)).getA() <= cutoff)
                                        isEdge = true;
                                }
                                if (countDiagonals) {
                                    for (int i = 0; i < 4; i++) {
                                        int x1 = DIAGONAL_X[i] + x;
                                        int y1 = DIAGONAL_Y[i] + y;
                                        if (ColorHolder.fromColorInt(inImg.getPixelRGBA(x1, y1)).getA() <= cutoff)
                                            isEdge = true;
                                    }
                                }
                            }
                        }

                        if (isEdge)
                            out.setPixelRGBA(x,y,0xFFFFFFFF);
                        else
                            out.setPixelRGBA(x,y,0);
                    }
                }
                return out;
            }
        };
    }
}
