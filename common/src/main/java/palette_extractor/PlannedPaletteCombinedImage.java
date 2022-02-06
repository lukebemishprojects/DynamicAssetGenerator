package palette_extractor;

import net.minecraft.resources.ResourceLocation;

public record PlannedPaletteCombinedImage(ResourceLocation background, ResourceLocation overlay, ResourceLocation paletted, boolean includeBackground, boolean extend) { }
