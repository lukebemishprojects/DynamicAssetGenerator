/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.NativeImageHelper;
import dev.lukebemish.dynamicassetgenerator.impl.client.util.SafeImageExtraction;
import dev.lukebemish.dynamicassetgenerator.impl.util.MultiCloser;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

public record AnimationSplittingSource(Map<String, TimeAwareSource> sources, ITexSource generator) implements ITexSource {
    public static final Codec<AnimationSplittingSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, TimeAwareSource.CODEC).fieldOf("sources").forGetter(AnimationSplittingSource::sources),
            ITexSource.CODEC.fieldOf("generator").forGetter(AnimationSplittingSource::generator)
    ).apply(instance, AnimationSplittingSource::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data, ResourceGenerationContext context) {
        Map<String, IoSupplier<NativeImage>> sources = new HashMap<>();
        Map<String, Integer> times = new HashMap<>();
        this.sources.forEach((key, source) -> {
            sources.put(key, source.source().getSupplier(data, context));
            times.put(key, source.scale());
        });
        if (sources.isEmpty()) {
            data.getLogger().error("No sources given...");
            return null;
        }
        return () -> {
            Map<String, NativeImage> images = new HashMap<>();
            for (Map.Entry<String, IoSupplier<NativeImage>> e : sources.entrySet()) {
                String key = e.getKey();
                images.put(key, e.getValue().get());
            }
            try (MultiCloser ignored = new MultiCloser(images.values())) {
                List<NativeImage> imageList = images.values().stream().toList();
                List<Integer> counts = images.entrySet().stream().map(entry->times.get(entry.getKey())*getFrameCount(entry.getValue())).toList();
                for (int j=0; j<counts.size(); j++) {
                    int i = counts.get(j);
                    if (i==0) {
                        data.getLogger().error("Source not shaped correctly for an animation...\n{}",imageList.get(j));
                        throw new IOException("Source not shaped correctly for an animation...");
                    }
                }
                int lcm = lcm(counts);
                int lcmWidth = lcm(imageList.stream().map(NativeImage::getWidth).toList());
                NativeImage output = NativeImageHelper.of(NativeImage.Format.RGBA, lcmWidth, lcmWidth*lcm, false);
                for (int i = 0; i < lcm; i++) {
                    Map<String, NativeImage> map = new HashMap<>();
                    int finalI = i;
                    images.forEach((str, old) -> map.put(str, getPartialImage(old, finalI, times.get(str))));
                    try (ImageCollection collection = new ImageCollection(map)) {
                        TexSourceDataHolder newData = new TexSourceDataHolder(data);
                        newData.put(ImageCollection.class, collection);
                        IoSupplier<NativeImage> supplier = generator.getSupplier(newData, context);
                        if (supplier == null) {
                            data.getLogger().error("Generator created no image...");
                            throw new IOException("Generator created no image...");
                        }
                        NativeImage supplied = supplier.get();
                        int sWidth = supplied.getWidth();
                        if (sWidth != supplied.getHeight()) {
                            data.getLogger().error("Generator created non-square image...\n{}",generator);
                            throw new IOException("Generator created non-square image...");
                        }
                        int scale = lcmWidth/sWidth;
                        for (int x = 0; x < lcmWidth; x++) {
                            for (int y = 0; y < lcmWidth; y++) {
                                int color = SafeImageExtraction.get(supplied, x/scale, y/scale);
                                output.setPixelRGBA(x,y+i*lcmWidth,color);
                            }
                        }
                    }
                }
                return output;
            }
        };
    }

    private static int gcd(int i1, int i2) {
        if (i1==0||i2==0)
            return i1+i2;
        int max = Math.max(i1,i2);
        int min = Math.min(i1,i2);
        return gcd(max % min, min);
    }

    private static int lcm(int i1, int i2) {
        return (i1*i2)/gcd(i1,i2);
    }

    @ApiStatus.Internal
    public static int lcm(List<Integer> ints) {
        if (ints.size() <= 1)
            return ints.get(0);
        if (ints.size() == 2)
            return lcm(ints.get(0),ints.get(1));
        List<Integer> newInts = new ArrayList<>(ints.subList(2,ints.size()));
        newInts.add(0,lcm(ints.get(0),ints.get(1)));
        return lcm(newInts);
    }

    private static int getFrameCount(NativeImage image) {
        return image.getHeight()/image.getWidth();
    }


    private static NativeImage getPartialImage(NativeImage input, int part, int scale) {
        int numFull = input.getHeight()/input.getWidth();
        int size = input.getWidth();
        NativeImage output = NativeImageHelper.of(input.format(), size, size, false);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                output.setPixelRGBA(x,y,SafeImageExtraction.get(input,x,((part/scale)%numFull)*size+y));
            }
        }
        return output;
    }

    public static class ImageCollection implements Closeable {
        private final Map<String, NativeImage> map;

        public ImageCollection(Map<String, NativeImage> map) {
            this.map = new HashMap<>(map);
        }

        @Override
        public void close() {
            map.values().forEach(NativeImage::close);
        }

        public NativeImage get(String key) {
            NativeImage input = map.get(key);
            NativeImage newImage = new NativeImage(input.format(), input.getWidth(), input.getHeight(), false);
            newImage.copyFrom(input);
            return newImage;
        }

        protected Collection<NativeImage> get() {
            return map.values();
        }
    }

    public record TimeAwareSource(ITexSource source, int scale) {
        public static final Codec<TimeAwareSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ITexSource.CODEC.fieldOf("source").forGetter(TimeAwareSource::source),
                Codec.INT.optionalFieldOf("scale",1).forGetter(TimeAwareSource::scale)
        ).apply(instance,TimeAwareSource::new));
    }
}
