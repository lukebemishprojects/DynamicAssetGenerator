package io.github.lukebemish.dynamic_asset_generator.api.client.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;

@Deprecated(forRemoval = true, since = "1.1.0")
public abstract class ForgeTextureConfigProvider extends TextureConfigProvider
{
    static final ExistingFileHelper.ResourceType TEXTURE = new ExistingFileHelper.ResourceType(PackType.CLIENT_RESOURCES, ".png", "textures");
    private final ExistingFileHelper fileHelper;

    public ForgeTextureConfigProvider(DataGenerator generator, ExistingFileHelper fileHelper, String modid) {
        super(generator, modid);
        this.fileHelper = fileHelper;
    }

    @Override
    protected boolean checkTextureExists(ResourceLocation texture) {
        return fileHelper.exists(texture, TEXTURE);
    }
}
