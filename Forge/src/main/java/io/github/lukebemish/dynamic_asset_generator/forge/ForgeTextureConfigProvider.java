package io.github.lukebemish.dynamic_asset_generator.forge;

import io.github.lukebemish.dynamic_asset_generator.datagen.TextureConfigProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;

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
