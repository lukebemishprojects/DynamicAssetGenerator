/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.mask;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.ResourceGenerationContext;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.NativeImageHelper;
import dev.lukebemish.dynamicassetgenerator.impl.client.palette.ColorHolder;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public record CutoffMask(Channel channel, ITexSource source, float cutoff) implements ITexSource {
    public static final Codec<CutoffMask> CODEC = RecordCodecBuilder.create(i->i.group(
            StringRepresentable.fromEnum(Channel::values).optionalFieldOf("channel",Channel.ALPHA).forGetter(CutoffMask::channel),
            ITexSource.CODEC.fieldOf("source").forGetter(CutoffMask::source),
            Codec.FLOAT.optionalFieldOf("cutoff",0.5f).forGetter(CutoffMask::cutoff)
    ).apply(i,CutoffMask::new));

    @Override
    public Codec<? extends ITexSource> codec() {
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
            try (NativeImage inImg = input.get()) {
                int width = inImg.getWidth();
                int height = inImg.getHeight();
                NativeImage out = NativeImageHelper.of(NativeImage.Format.RGBA, width, height, false);
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < width; y++) {
                        ColorHolder source = ColorHolder.fromColorInt(inImg.getPixelRGBA(x,y));
                        float value = channel.getter.apply(source);
                        out.setPixelRGBA(x,y,value>cutoff ? 0xffffffff : 0);
                    }
                }
                return out;
            }
        };
    }

    enum Channel implements StringRepresentable {
        ALPHA("alpha", ColorHolder::getA),
        RED("red", ColorHolder::getR),
        GREEN("green", ColorHolder::getG),
        BLUE("blue", ColorHolder::getG),
        HUE("hue",c->c.toHLS().getH()),
        SATURATION("saturation",c->c.toHLS().getS()),
        LIGHTNESS("lightness",c->c.toHLS().getL());

        public final String name;
        public final Function<ColorHolder, Float> getter;

        Channel(String name, Function<ColorHolder, Float> getter) {
            this.name = name;
            this.getter = getter;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
