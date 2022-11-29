/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.palette.Palette;
import dev.lukebemish.dynamicassetgenerator.impl.client.util.IPalettePlan;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class CombinedPaletteImage implements ITexSource {
    public static final Codec<CombinedPaletteImage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.CODEC.fieldOf("overlay").forGetter(s->s.overlay),
            ITexSource.CODEC.fieldOf("background").forGetter(s->s.background),
            ITexSource.CODEC.fieldOf("paletted").forGetter(s->s.paletted),
            Codec.BOOL.fieldOf("include_background").forGetter(s->s.includeBackground),
            Codec.BOOL.fieldOf("stretch_paletted").forGetter(s->s.stretchPaletted),
            Codec.INT.fieldOf("extend_palette_size").forGetter(s->s.extendPaletteSize)
    ).apply(instance,CombinedPaletteImage::new));

    @Override
    public Codec<CombinedPaletteImage> codec() {
        return CODEC;
    }

    @Override
    public @NotNull Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException {
        PalettePlanner planner = PalettePlanner.of(this, data);
        if (planner == null) return ()->null;
        return () -> {
            try (NativeImage bImg = background.getSupplier(data).get();
                 NativeImage oImg = overlay.getSupplier(data).get();
                 NativeImage pImg = paletted.getSupplier(data).get()) {
                boolean good = true;
                if (bImg == null) {
                    data.getLogger().error("Background image was none... \n{}", background);
                    good = false;
                }
                if (oImg == null) {
                    data.getLogger().error("Overlay image was none... \n{}", overlay);
                    good = false;
                }
                if (pImg == null) {
                    data.getLogger().error("Paletted image was none... \n{}", paletted);
                    good = false;
                }
                if (good)
                    return Palette.paletteCombinedImage(bImg, oImg, pImg, planner);
                return null;
            }
        };
    }

    private final ITexSource overlay;
    private final ITexSource background;
    private final ITexSource paletted;
    private final boolean includeBackground;
    private final boolean stretchPaletted;
    private final int extendPaletteSize;

    public CombinedPaletteImage(ITexSource overlay, ITexSource background, ITexSource paletted, boolean includeBackground, boolean stretchPaletted, int extendPaletteSize) {
        this.overlay = overlay;
        this.background = background;
        this.paletted = paletted;
        this.includeBackground = includeBackground;
        this.stretchPaletted = stretchPaletted;
        this.extendPaletteSize = extendPaletteSize;
    }

    public static class PalettePlanner implements IPalettePlan {
        private final CombinedPaletteImage info;
        private Supplier<NativeImage> overlay;
        private Supplier<NativeImage> background;
        private Supplier<NativeImage> paletted;

        private PalettePlanner(CombinedPaletteImage info) {
            this.info = info;
        }

        public static PalettePlanner of(CombinedPaletteImage info, TexSourceDataHolder data) {
            PalettePlanner out = new PalettePlanner(info);
            out.background = info.background.getSupplier(data);
            out.paletted = info.paletted.getSupplier(data);
            out.overlay = info.overlay.getSupplier(data);
            return out;
        }

        @Override
        public boolean includeBackground() {
            return info.includeBackground;
        }

        @Override
        public boolean stretchPaletted() {
            return info.stretchPaletted;
        }

        @Override
        public int extend() {
            return info.extendPaletteSize;
        }
    }
}
