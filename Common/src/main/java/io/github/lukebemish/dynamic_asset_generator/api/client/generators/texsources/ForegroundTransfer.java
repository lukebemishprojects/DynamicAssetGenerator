package io.github.lukebemish.dynamic_asset_generator.api.client.generators.texsources;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.api.client.generators.TexSourceDataHolder;
import io.github.lukebemish.dynamic_asset_generator.impl.client.PaletteExtractor;
import io.github.lukebemish.dynamic_asset_generator.impl.client.palette.Palette;
import io.github.lukebemish.dynamic_asset_generator.impl.client.util.IPalettePlan;

import java.util.function.Supplier;

public record ForegroundTransfer(ITexSource background, ITexSource full, ITexSource newBackground,
                                 int extendPaletteSize, boolean trimTrailing, boolean forceNeighbors, boolean fillHoles,
                                 double closeCutoff) implements ITexSource {
    public static final Codec<ForegroundTransfer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITexSource.CODEC.fieldOf("background").forGetter(ForegroundTransfer::background),
            ITexSource.CODEC.fieldOf("full").forGetter(ForegroundTransfer::full),
            ITexSource.CODEC.fieldOf("new_background").forGetter(ForegroundTransfer::newBackground),
            Codec.INT.fieldOf("extend_palette_size").forGetter(ForegroundTransfer::extendPaletteSize),
            Codec.BOOL.fieldOf("trim_trailing").forGetter(ForegroundTransfer::trimTrailing),
            Codec.BOOL.fieldOf("force_neighbors").forGetter(ForegroundTransfer::forceNeighbors),
            Codec.BOOL.fieldOf("fill_holes").forGetter(ForegroundTransfer::fillHoles),
            Codec.DOUBLE.fieldOf("close_cutoff").forGetter(ForegroundTransfer::closeCutoff)
    ).apply(instance, ForegroundTransfer::new));

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public Supplier<NativeImage> getSupplier(TexSourceDataHolder data) throws JsonSyntaxException {
        Supplier<NativeImage> background = this.background().getSupplier(data);
        Supplier<NativeImage> newBackground = this.newBackground().getSupplier(data);
        Supplier<NativeImage> full = this.full().getSupplier(data);

        return () -> {
            if (background == null || full == null || newBackground == null) {
                DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...");
                return null;
            }
            try (NativeImage bImg = background.get();
                 NativeImage nImg = newBackground.get();
                 NativeImage fImg = full.get()) {
                if (bImg == null) {
                    DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", this.background());
                    return null;
                }
                if (nImg == null) {
                    DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", this.newBackground());
                    return null;
                }
                if (fImg == null) {
                    DynamicAssetGenerator.LOGGER.error("Texture given was nonexistent...\n{}", this.full());
                    return null;
                }
                try (PaletteExtractor extractor = new PaletteExtractor(bImg, fImg, this.extendPaletteSize(), this.trimTrailing(), this.forceNeighbors(), this.closeCutoff()).fillHoles(this.fillHoles());) {
                    PalettePlanner planner = PalettePlanner.of(extractor, nImg);
                    extractor.recalcImages();
                    try (NativeImage pImg = extractor.getPalettedImg();
                         NativeImage oImg = extractor.getOverlayImg()) {
                        return Palette.paletteCombinedImage(nImg, oImg, pImg, planner);
                    }
                }
            }
        };
    }

    public static class PalettePlanner implements IPalettePlan {
        private final PaletteExtractor extractor;

        private PalettePlanner(PaletteExtractor extractor) {
            this.extractor = extractor;
        }

        public static PalettePlanner of(PaletteExtractor extractor, NativeImage nImg) {
            return new PalettePlanner(extractor);
        }

        @Override
        public boolean includeBackground() {
            return true;
        }

        @Override
        public boolean stretchPaletted() {
            return false;
        }

        @Override
        public int extend() {
            return extractor.extend;
        }
    }
}
