/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSourceDataHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.palette.Palette;
import dev.lukebemish.dynamicassetgenerator.impl.client.util.IPalettePlan;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

public record CombinedPaletteImage(ITexSource overlay, ITexSource background, ITexSource paletted, boolean includeBackground, boolean stretchPaletted, int extendPaletteSize) implements ITexSource {
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
    public @Nullable IoSupplier<NativeImage> getSupplier(TexSourceDataHolder data) {
        PalettePlanner planner = PalettePlanner.of(this, data);
        if (planner.background == null) {
            data.getLogger().error("Background image was none... \n{}", background);
            return null;
        }
        if (planner.overlay == null) {
            data.getLogger().error("Overlay image was none... \n{}", overlay);
            return null;
        }
        if (planner.paletted == null) {
            data.getLogger().error("Paletted image was none... \n{}", paletted);
            return null;
        }
        return () -> {
            try (NativeImage bImg = planner.background.get();
                 NativeImage oImg = planner.overlay.get();
                 NativeImage pImg = planner.paletted.get()) {
                return Palette.paletteCombinedImage(bImg, oImg, pImg, planner);
            }
        };
    }

    public static class PalettePlanner implements IPalettePlan {
        private final CombinedPaletteImage info;
        private IoSupplier<NativeImage> overlay;
        private IoSupplier<NativeImage> background;
        private IoSupplier<NativeImage> paletted;

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
