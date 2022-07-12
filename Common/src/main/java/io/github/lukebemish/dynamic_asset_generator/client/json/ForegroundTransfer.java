package io.github.lukebemish.dynamic_asset_generator.client.json;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.dynamic_asset_generator.DynamicAssetGenerator;
import io.github.lukebemish.dynamic_asset_generator.client.api.PaletteExtractor;
import io.github.lukebemish.dynamic_asset_generator.client.api.json.DynamicTextureJson;
import io.github.lukebemish.dynamic_asset_generator.client.api.json.ITexSource;
import io.github.lukebemish.dynamic_asset_generator.client.palette.Palette;
import io.github.lukebemish.dynamic_asset_generator.client.util.IPalettePlan;

import java.io.IOException;
import java.util.function.Supplier;

public record ForegroundTransfer(ITexSource background, ITexSource full, ITexSource newBackground,
                                 int extendPaletteSize, boolean trimTrailing, boolean forceNeighbors, boolean fillHoles,
                                 double closeCutoff) implements ITexSource {
    public static final Codec<ForegroundTransfer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DynamicTextureJson.TEXSOURCE_CODEC.fieldOf("background").forGetter(ForegroundTransfer::background),
            DynamicTextureJson.TEXSOURCE_CODEC.fieldOf("full").forGetter(ForegroundTransfer::full),
            DynamicTextureJson.TEXSOURCE_CODEC.fieldOf("new_background").forGetter(ForegroundTransfer::newBackground),
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
    public Supplier<NativeImage> getSupplier() throws JsonSyntaxException {
        Supplier<NativeImage> background = this.background().getSupplier();
        Supplier<NativeImage> newBackground = this.newBackground().getSupplier();
        Supplier<NativeImage> full = this.full().getSupplier();

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
                PaletteExtractor extractor = new PaletteExtractor(() -> bImg, () -> fImg, this.extendPaletteSize(), this.trimTrailing(), this.forceNeighbors(), this.closeCutoff()).fillHoles(this.fillHoles());
                PalettePlanner planner = PalettePlanner.of(this, extractor, nImg);
                return Palette.paletteCombinedImage(planner);
            }
        };
    }

    public static class PalettePlanner implements IPalettePlan {
        private final ForegroundTransfer info;
        private final NativeImage nImg;
        private final PaletteExtractor extractor;

        private PalettePlanner(ForegroundTransfer info, PaletteExtractor extractor, NativeImage nImg) {
            this.info = info;
            this.extractor = extractor;
            this.nImg = nImg;
        }

        public static PalettePlanner of(ForegroundTransfer info, PaletteExtractor extractor, NativeImage nImg) {
            return new PalettePlanner(info, extractor, nImg);
        }

        @Override
        public NativeImage getBackground() throws IOException {
            return nImg;
        }

        @Override
        public NativeImage getOverlay() throws IOException {
            return extractor.getOverlayImg();
        }

        @Override
        public NativeImage getPaletted() throws IOException {
            return extractor.getPalettedImg();
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
