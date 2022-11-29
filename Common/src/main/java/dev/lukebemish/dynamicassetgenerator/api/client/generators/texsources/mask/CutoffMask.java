/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.mask;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.NativeImageHelper;
import dev.lukebemish.dynamicassetgenerator.impl.client.palette.ColorHolder;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

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
    public @NotNull Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException {
        Supplier<NativeImage> input = this.source.getSupplier(data);
        return () -> {
            try (NativeImage inImg = input.get()) {
                if (inImg == null) {
                    data.getLogger().error("Texture given was nonexistent...\n{}", this.source);
                    return null;
                }
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
        ALPHA("alpha",c->c.getA()),
        RED("red",c->c.getR()),
        GREEN("green",c->c.getG()),
        BLUE("blue",c->c.getG()),
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
        public String getSerializedName() {
            return name;
        }
    }
}
