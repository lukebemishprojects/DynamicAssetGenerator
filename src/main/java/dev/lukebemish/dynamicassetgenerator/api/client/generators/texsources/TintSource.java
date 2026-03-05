package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.ColorSource;
import dev.lukebemish.dynamicassetgenerator.api.client.image.ImageUtils;
import dev.lukebemish.dynamicassetgenerator.api.colors.ColorEncoding;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.FastColor;
import net.minecraft.util.StringRepresentable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class TintSource implements TexSource {
    public static final MapCodec<TintSource> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            TexSource.CODEC.fieldOf("source").forGetter(TintSource::getSource),
            Codec.either(Codec.STRING, Codec.INT).flatXmap(e -> e.map(s -> {
                try {
                    return DataResult.success(Integer.parseInt(s));
                } catch (NumberFormatException ex) {
                    return DataResult.error(() -> "Not an integer: " + s);
                }
            }, DataResult::success), i -> DataResult.success(Either.right(i))).fieldOf("color").forGetter(s -> s.color),
            StringRepresentable.fromEnum(ColorEncoding::values).optionalFieldOf("encoding", ColorSource.DEFAULT_COLOR_ENCODING).forGetter(TintSource::getColorEncoding)
    ).apply(inst, TintSource::new));

    private final TexSource source;
    private final int color;
    private final ColorEncoding colorEncoding;

    private TintSource(TexSource source, int color, ColorEncoding colorEncoding) {
        this.source = source;
        this.color = color;
        this.colorEncoding = colorEncoding;
    }

    @Override
    public @NonNull MapCodec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> source = getSource().getCachedSupplier(data, context);
        if (source == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.getSource().stringify());
            return null;
        }
        return () -> {
            try (NativeImage image = source.get()) {
                int abgrColor = colorEncoding.toABGR.applyAsInt(color);

                PointwiseOperation.Unary<Integer> operation = (c, i) -> {
                    if (!i) return 0;
                    return FastColor.ARGB32.color(
                            FastColor.ARGB32.alpha(c) * FastColor.ABGR32.alpha(abgrColor) / 255,
                            FastColor.ARGB32.red(c) * FastColor.ABGR32.red(abgrColor) / 255,
                            FastColor.ARGB32.green(c) * FastColor.ABGR32.green(abgrColor) / 255,
                            FastColor.ARGB32.blue(c) * FastColor.ABGR32.blue(abgrColor) / 255
                    );
                };

                return ImageUtils.generateScaledImage(operation, List.of(image));
            }
        };
    }

    public TexSource getSource() {
        return source;
    }

    public int getColor() {
        return color;
    }

    public ColorEncoding getColorEncoding() {
        return colorEncoding;
    }

    public static class Builder {
        private TexSource source;
        private Integer color;
        private ColorEncoding colorEncoding = ColorSource.DEFAULT_COLOR_ENCODING;

        public Builder setSource(TexSource source) {
            this.source = source;
            return this;
        }

        public Builder setColor(int color) {
            this.color = color;
            return this;
        }

        public Builder setColorEncoding(ColorEncoding colorEncoding) {
            this.colorEncoding = colorEncoding;
            return this;
        }

        public TintSource build() {
            Objects.requireNonNull(source);
            Objects.requireNonNull(color);
            return new TintSource(source, color, colorEncoding);
        }
    }
}
