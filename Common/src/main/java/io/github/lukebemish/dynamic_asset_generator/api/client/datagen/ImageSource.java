package io.github.lukebemish.dynamic_asset_generator.api.client.datagen;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Deprecated(forRemoval = true, since = "1.1.0")
public abstract class ImageSource {
    private final TextureConfigProvider provider;
    private final String type;

    protected ImageSource(TextureConfigProvider provider, String type) {
        this.provider = provider;
        this.type = type;
    }

    protected final void checkExists(ResourceLocation texture) {
        Preconditions.checkArgument(
                provider.checkTextureExists(texture),
                "Texture at %s does not exist",
                texture
        );
    }

    @MustBeInvokedByOverriders
    JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", type);
        return object;
    }

    public static final class File extends ImageSource {
        private final ResourceLocation path;

        File(TextureConfigProvider provider, ResourceLocation path) {
            super(provider, DynamicAssetGenerator.MOD_ID + ":texture");
            Preconditions.checkNotNull(path, "Path must not be null");
            checkExists(path);
            this.path = path;
        }

        @Override
        JsonObject toJson() {
            Preconditions.checkNotNull(path, "No texture path set");

            JsonObject object = super.toJson();
            object.addProperty("path", path.toString());
            return object;
        }
    }

    public static final class Fallback extends ImageSource {
        private ImageSource original = null;
        private ImageSource fallback = null;

        Fallback(TextureConfigProvider provider) { super(provider, DynamicAssetGenerator.MOD_ID + ":fallback"); }

        public Fallback fallback(ImageSource fallback) {
            Preconditions.checkNotNull(fallback, "Fallback source most not be null");
            this.fallback = fallback;
            return this;
        }

        public Fallback original(ImageSource original) {
            Preconditions.checkNotNull(original, "Original source most not be null");
            this.original = original;
            return this;
        }

        @Override
        JsonObject toJson() {
            Preconditions.checkNotNull(original, "No original source set");
            Preconditions.checkNotNull(fallback, "No fallback source set");

            JsonObject object = super.toJson();
            object.add("original", original.toJson());
            object.add("fallback", fallback.toJson());
            return object;
        }
    }

    public static final class Color extends ImageSource {
        private final List<Integer> colors = new ArrayList<>();
        
        Color(TextureConfigProvider provider) { super(provider, DynamicAssetGenerator.MOD_ID + ":color"); }

        public Color color(int color) {
            colors.add(color);
            return this;
        }

        @Override
        JsonObject toJson() {
            JsonArray colorArray = new JsonArray(colors.size());
            colors.forEach(colorArray::add);

            JsonObject object = super.toJson();
            object.add("color", colorArray);
            return object;
        }
    }

    public static final class Overlay extends ImageSource {
        private final List<ImageSource> inputs = new ArrayList<>();

        Overlay(TextureConfigProvider provider) { super(provider, DynamicAssetGenerator.MOD_ID + ":overlay"); }

        public Overlay input(ImageSource input) {
            Preconditions.checkNotNull(input, "Input source most not be null");
            inputs.add(input);
            return this;
        }

        @Override
        JsonObject toJson() {
            JsonArray inputArray = new JsonArray(inputs.size());
            inputs.forEach(input -> inputArray.add(input.toJson()));

            JsonObject object = super.toJson();
            object.add("inputs", inputArray);
            return object;
        }
    }

    public static final class Mask extends ImageSource {
        private ImageSource mask = null;
        private ImageSource input = null;

        Mask(TextureConfigProvider provider) { super(provider, DynamicAssetGenerator.MOD_ID + ":mask"); }

        public Mask mask(ImageSource mask) {
            Preconditions.checkNotNull(mask, "Mask source most not be null");
            this.mask = mask;
            return this;
        }

        public Mask input(ImageSource input) {
            Preconditions.checkNotNull(input, "Input source most not be null");
            this.input = input;
            return this;
        }

        @Override
        JsonObject toJson() {
            Preconditions.checkNotNull(mask, "No mask source set");
            Preconditions.checkNotNull(input, "No input source set");

            JsonObject object = super.toJson();
            object.add("mask", mask.toJson());
            object.add("input", input.toJson());
            return object;
        }
    }

    public static final class Crop extends ImageSource {
        private ImageSource input = null;
        private int totalWidth = -1;
        private int startX = 0;
        private int startY = 0;
        private int sizeX = -1;
        private int sizeY = -1;

        Crop(TextureConfigProvider provider) { super(provider, DynamicAssetGenerator.MOD_ID + ":crop"); }

        public Crop input(ImageSource input) {
            Preconditions.checkNotNull(input, "Input source most not be null");
            this.input = input;
            return this;
        }

        public Crop totalWidth(int totalWidth) {
            Preconditions.checkArgument(totalWidth > 0, "Total width must be > 0");
            this.totalWidth = totalWidth;
            return this;
        }

        public Crop startX(int startX) {
            this.startX = startX;
            return this;
        }

        public Crop startY(int startY) {
            this.startY = startY;
            return this;
        }

        public Crop sizeX(int sizeX) {
            Preconditions.checkArgument(sizeX > 0, "Size X must be > 0");
            this.sizeX = sizeX;
            return this;
        }

        public Crop sizeY(int sizeY) {
            Preconditions.checkArgument(sizeY > 0, "Size Y must be > 0");
            this.sizeY = sizeY;
            return this;
        }

        @Override
        JsonObject toJson() {
            Preconditions.checkNotNull(input, "No input source set");
            Preconditions.checkState(totalWidth != -1, "No total width set");
            Preconditions.checkState(sizeX != -1, "No size X set");
            Preconditions.checkState(sizeY != -1, "No size Y set");

            JsonObject object = super.toJson();
            object.add("input", input.toJson());
            object.addProperty("total_width", totalWidth);
            object.addProperty("start_x", startX);
            object.addProperty("start_y", startY);
            object.addProperty("size_x", sizeX);
            object.addProperty("size_y", sizeY);
            return object;
        }
    }

    public static final class Transform extends ImageSource {
        private ImageSource input = null;
        private int rotate = 0;
        private boolean flip = false;

        Transform(TextureConfigProvider provider) { super(provider, DynamicAssetGenerator.MOD_ID + ":transform"); }

        public Transform input(ImageSource input) {
            Preconditions.checkNotNull(input, "Input source most not be null");
            this.input = input;
            return this;
        }

        public Transform rotate(int rotate) {
            this.rotate = rotate;
            return this;
        }

        public Transform flip(boolean flip) {
            this.flip = flip;
            return this;
        }

        @Override
        JsonObject toJson() {
            Preconditions.checkNotNull(input, "No input source set");

            JsonObject object = super.toJson();
            object.add("input", input.toJson());
            object.addProperty("rotate", rotate);
            object.addProperty("flip", flip);
            return object;
        }
    }

    public static final class CombinedPalettedImage extends ImageSource {
        private ImageSource overlay = null;
        private ImageSource background = null;
        private ImageSource paletted = null;
        private boolean includeBackground = false;
        private boolean stretchPaletted = false;
        private int extendPaletteSize = 0;

        CombinedPalettedImage(TextureConfigProvider provider) {
            super(provider, DynamicAssetGenerator.MOD_ID + ":combined_paletted_image");
        }

        public CombinedPalettedImage overlay(ImageSource overlay) {
            Preconditions.checkNotNull(overlay, "Overlay source most not be null");
            this.overlay = overlay;
            return this;
        }

        public CombinedPalettedImage background(ImageSource background) {
            Preconditions.checkNotNull(background, "Background source most not be null");
            this.background = background;
            return this;
        }

        public CombinedPalettedImage paletted(ImageSource paletted) {
            Preconditions.checkNotNull(paletted, "Paletted source most not be null");
            this.paletted = paletted;
            return this;
        }

        public CombinedPalettedImage includeBackground(boolean includeBackground) {
            this.includeBackground = includeBackground;
            return this;
        }

        public CombinedPalettedImage stretchPaletted(boolean stretchPaletted) {
            this.stretchPaletted = stretchPaletted;
            return this;
        }

        public CombinedPalettedImage extendPaletteSize(int extendPaletteSize) {
            this.extendPaletteSize = extendPaletteSize;
            return this;
        }

        @Override
        JsonObject toJson() {
            Preconditions.checkNotNull(overlay, "No overlay source set");
            Preconditions.checkNotNull(background, "No background source set");
            Preconditions.checkNotNull(paletted, "No paletted source set");

            JsonObject object = super.toJson();
            object.add("overlay", overlay.toJson());
            object.add("background", background.toJson());
            object.add("paletted", paletted.toJson());
            object.addProperty("include_background", includeBackground);
            object.addProperty("stretch_paletted", stretchPaletted);
            object.addProperty("extend_palette_size", extendPaletteSize);
            return object;
        }
    }

    public static final class ForegroundTransfer extends ImageSource {
        private ImageSource background = null;
        private ImageSource full = null;
        private ImageSource newBackground = null;
        private boolean trimTrailing = false;
        private boolean forceNeighbors = false;
        private boolean fillHoles = false;
        private int extendPaletteSize = 0;
        private float closeCutoff = 0F;

        ForegroundTransfer(TextureConfigProvider provider) { super(provider, DynamicAssetGenerator.MOD_ID + ":foreground_transfer"); }

        public ForegroundTransfer background(ImageSource background) {
            Preconditions.checkNotNull(background, "Background source most not be null");
            this.background = background;
            return this;
        }

        public ForegroundTransfer full(ImageSource full) {
            Preconditions.checkNotNull(full, "Full source most not be null");
            this.full = full;
            return this;
        }

        public ForegroundTransfer newBackground(ImageSource newBackground) {
            Preconditions.checkNotNull(newBackground, "New Background source most not be null");
            this.newBackground = newBackground;
            return this;
        }

        public ForegroundTransfer trimTrailing(boolean trimTrailing) {
            this.trimTrailing = trimTrailing;
            return this;
        }

        public ForegroundTransfer forceNeighbors(boolean forceNeighbors) {
            this.forceNeighbors = forceNeighbors;
            return this;
        }

        public ForegroundTransfer fillHoles(boolean fillHoles) {
            this.fillHoles = fillHoles;
            return this;
        }

        public ForegroundTransfer extendPaletteSize(int extendPaletteSize) {
            Preconditions.checkArgument(extendPaletteSize >= 0, "Extend Palette Size must be >= 0");
            this.extendPaletteSize = extendPaletteSize;
            return this;
        }

        public ForegroundTransfer extendPaletteSize(float closeCutoff) {
            Preconditions.checkArgument(closeCutoff >= 0F && closeCutoff <= 1F, "Close Cutoff must be between 0 and 1");
            this.closeCutoff = closeCutoff;
            return this;
        }

        @Override
        JsonObject toJson() {
            Preconditions.checkNotNull(background, "No background source set");
            Preconditions.checkNotNull(full, "No full source set");
            Preconditions.checkNotNull(newBackground, "No new background source set");

            JsonObject object = super.toJson();
            object.add("background", background.toJson());
            object.add("full", full.toJson());
            object.add("new_background", newBackground.toJson());
            object.addProperty("trim_trailing", trimTrailing);
            object.addProperty("force_neighbors", forceNeighbors);
            object.addProperty("fill_holes", fillHoles);
            object.addProperty("extend_palette_size", extendPaletteSize);
            object.addProperty("close_cutoff", closeCutoff);
            return object;
        }
    }
}
