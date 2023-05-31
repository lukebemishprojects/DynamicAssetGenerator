package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.mask;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.api.client.image.ImageUtils;
import dev.lukebemish.dynamicassetgenerator.api.colors.Channel;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public record ChannelMask(TexSource source, Channel channel) implements TexSource {
    public static final Codec<ChannelMask> CODEC = RecordCodecBuilder.create(i -> i.group(
            TexSource.CODEC.fieldOf("source").forGetter(ChannelMask::source),
            Channel.CODEC.fieldOf("channel").forGetter(ChannelMask::channel)
    ).apply(i, ChannelMask::new));

    @Override
    public Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        IoSupplier<NativeImage> input = this.source.getSupplier(data, context);
        if (input == null) {
            data.getLogger().error("Texture given was nonexistent...\n{}", this.source);
            return null;
        }
        return () -> {
            PointwiseOperation.Unary<Integer> operation = PointwiseOperation.Unary.chain(
                    channel.makeOperation(),
                    (c, i) -> ((c & 0xFF) << 24) | 0xFFFFFF
            );
            try (NativeImage inImg = input.get()) {
                return ImageUtils.generateScaledImage(operation, List.of(inImg));
            }
        };
    }
}
