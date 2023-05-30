package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.api.client.image.ImageUtils;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;
import dev.lukebemish.dynamicassetgenerator.impl.util.MultiCloser;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

abstract public class AbstractManyOperationSource implements TexSource {
    private final List<TexSource> sources;

    public AbstractManyOperationSource(List<TexSource> sources) {
        this.sources = sources;
    }

    public List<TexSource> getSources() {
        return sources;
    }

    public abstract PointwiseOperation.Any<Integer> getOperation();

    public static <T extends AbstractManyOperationSource> Codec<T> makeCodec(Function<List<TexSource>, T> ctor) {
        return RecordCodecBuilder.create(instance -> instance.group(
                TexSource.CODEC.listOf().fieldOf("sources").forGetter(AbstractManyOperationSource::getSources)
        ).apply(instance, ctor));
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        List<IoSupplier<NativeImage>> inputs = new ArrayList<>();
        for (TexSource o : this.getSources()) {
            var source = o.getSupplier(data, context);
            if (source == null) {
                data.getLogger().error("Texture given was nonexistent...\n{}",o);
                return null;
            }
            inputs.add(source);
        }
        return () -> {
            List<NativeImage> images = new ArrayList<>();
            for (var input : inputs) {
                images.add(input.get());
            }
            try (MultiCloser ignored = new MultiCloser(images)) {
                return ImageUtils.generateScaledImage(getOperation(), images);
            }
        };
    }
}
