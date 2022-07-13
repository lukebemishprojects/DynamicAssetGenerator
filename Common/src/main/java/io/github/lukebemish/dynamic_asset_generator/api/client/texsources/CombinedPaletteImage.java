package io.github.lukebemish.dynamic_asset_generator.api.client.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.api.client.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.client.palette.Palette;
import io.github.lukebemish.dynamic_asset_generator.client.util.IPalettePlan;

import java.io.IOException;
import java.util.function.Supplier;

public class CombinedPaletteImage implements ITexSource {
    public static final Codec<CombinedPaletteImage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.TEXSOURCE_CODEC.fieldOf("overlay").forGetter(s->s.overlay),
            ITexSource.TEXSOURCE_CODEC.fieldOf("background").forGetter(s->s.background),
            ITexSource.TEXSOURCE_CODEC.fieldOf("paletted").forGetter(s->s.paletted),
            Codec.BOOL.fieldOf("include_background").forGetter(s->s.includeBackground),
            Codec.BOOL.fieldOf("stretch_paletted").forGetter(s->s.stretchPaletted),
            Codec.INT.fieldOf("extend_palette_size").forGetter(s->s.extendPaletteSize)
    ).apply(instance,CombinedPaletteImage::new));

    public Codec<CombinedPaletteImage> codec() {
        return CODEC;
    }

    @Override
    public Supplier<NativeImage> getSupplier() throws JsonSyntaxException {
        PalettePlanner planner = PalettePlanner.of(this);
        if (planner == null) return null;
        return () -> Palette.paletteCombinedImage(planner);
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

        public static PalettePlanner of(CombinedPaletteImage info) {
            PalettePlanner out = new PalettePlanner(info);
            out.background = info.background.getSupplier();
            out.paletted = info.paletted.getSupplier();
            out.overlay = info.overlay.getSupplier();
            if (out.overlay == null || out.background == null || out.paletted == null) return null;
            return out;
        }

        @Override
        public NativeImage getBackground() throws IOException {
            return background.get();
        }

        @Override
        public NativeImage getOverlay() throws IOException {
            return overlay.get();
        }

        @Override
        public NativeImage getPaletted() throws IOException {
            return paletted.get();
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
