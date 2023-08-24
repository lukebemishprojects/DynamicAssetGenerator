package dev.lukebemish.dynamicassetgenerator.impl.util;

import dev.lukebemish.dynamicassetgenerator.api.PathAwareInputStreamSource;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.Timing;
import dev.lukebemish.dynamicassetgenerator.impl.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.BiFunction;

public final class ResourceUtils {
    private ResourceUtils() {}

    public static IoSupplier<InputStream> wrapSafeData(ResourceLocation rl, PathAwareInputStreamSource source, ResourceGenerationContext context) {
        return wrapSafeData(
            rl,
            source::get,
            context,
            i -> i,
            i -> i,
            source::createCacheKey
        );
    }

    public static <T extends AutoCloseable> IoSupplier<T> wrapSafeData(
        ResourceLocation rl,
        BiFunction<ResourceLocation, ResourceGenerationContext, IoSupplier<T>> source,
        ResourceGenerationContext context,
        IoFunction<T, InputStream> writer,
        IoFunction<InputStream, T> opener,
        BiFunction<ResourceLocation, ResourceGenerationContext, String> cacheKeyMaker
    ) {
        IoSupplier<T> supplier = null;
        Transformer<T> transformer = is -> is;
        if (DynamicAssetGenerator.getConfig().fullCache()) {
            try {
                Path path = DynamicAssetGenerator.cache(context.getCacheName(), false).resolve(rl.getNamespace()).resolve(rl.getPath());
                if (!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
                if (Files.exists(path)) {
                    supplier = () -> opener.apply(new BufferedInputStream(Files.newInputStream(path)));
                } else {
                    transformer = transformer.andThen(is -> {
                        try (var stream = is) {
                            Files.copy(writer.apply(stream), path, StandardCopyOption.REPLACE_EXISTING);
                            return opener.apply(new BufferedInputStream(Files.newInputStream(path)));
                        } catch (IOException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new IOException(e);
                        }
                    });
                }
            } catch (IOException e) {
                DynamicAssetGenerator.LOGGER.error("Could not cache resource {}...", rl, e);
            }
        } else if (DynamicAssetGenerator.getConfig().keyedCache()) {
            String partialCacheKey = cacheKeyMaker.apply(rl, context);
            if (partialCacheKey != null) {
                String cacheKey = Services.PLATFORM.getModVersion()+":"+partialCacheKey;
                Path keyPath = DynamicAssetGenerator.cache(context.getCacheName(), true).resolve(rl.getNamespace()).resolve(rl.getPath() + ".dynassetgen");
                Path contentPath = DynamicAssetGenerator.cache(context.getCacheName(), true).resolve(rl.getNamespace()).resolve(rl.getPath());
                try {
                    if (!Files.exists(keyPath.getParent())) Files.createDirectories(keyPath.getParent());
                    String existingKey = null;
                    if (Files.exists(keyPath)) {
                        existingKey = Files.readString(keyPath, StandardCharsets.UTF_8);
                    }
                    if (existingKey != null && existingKey.equals(cacheKey)) {
                        supplier = () -> opener.apply(new BufferedInputStream(Files.newInputStream(contentPath)));
                    } else {
                        supplier = source.apply(rl, context);
                        transformer = transformer.andThen(is -> {
                            try (var stream = is) {
                                Files.copy(writer.apply(stream), contentPath, StandardCopyOption.REPLACE_EXISTING);
                                Files.writeString(keyPath, cacheKey, StandardCharsets.UTF_8);
                                return opener.apply(new BufferedInputStream(Files.newInputStream(contentPath)));
                            } catch (IOException e) {
                                throw e;
                            } catch (Exception e) {
                                throw new IOException(e);
                            }
                        });
                    }
                } catch (IOException e) {
                    DynamicAssetGenerator.LOGGER.error("Could not cache resource {}...", rl, e);
                    supplier = source.apply(rl, context);
                }
            }
        }
        if (supplier == null) {
            supplier = source.apply(rl, context);
        }
        if (supplier == null) return null;
        IoSupplier<T> finalSupplier = supplier;
        Transformer<T> finalTransformer = transformer;
        IoSupplier<T> output = () -> {
            try {
                return finalTransformer.transform(finalSupplier.get());
            } catch (Throwable e) {
                DynamicAssetGenerator.LOGGER.error("Issue reading supplying resource {}:", rl, e);
                throw new IOException(e);
            }
        };
        if (DynamicAssetGenerator.TIME_RESOURCES) {
            return () -> {
                long startTime = System.nanoTime();
                var result = output.get();
                long endTime = System.nanoTime();

                long duration = (endTime - startTime)/1000;
                Timing.recordTime(context.getCacheName().toString(), rl, duration);
                return result;
            };
        }
        return output;
    }

    private interface Transformer<T extends AutoCloseable> {
        T transform(T stream) throws IOException;

        default Transformer<T> andThen(Transformer<T> after) {
            return stream -> after.transform(transform(stream));
        }
    }

    public interface IoFunction<T, R> {
        R apply(T stream) throws IOException;
    }
}
