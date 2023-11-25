package dev.lukebemish.dynamicassetgenerator.impl.mixin;

import dev.lukebemish.dynamicassetgenerator.api.client.SpriteProvider;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(SpriteSourceList.class)
public class SpriteSourceListMixin {
    @ModifyVariable(
        method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/texture/atlas/SpriteSourceList;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/atlas/SpriteSourceList;<init>(Ljava/util/List;)V",
            shift = At.Shift.BEFORE
        ),
        ordinal = 0
    )
    private static List<SpriteSource> dynamic_asset_generator$onLoad(List<SpriteSource> sources, ResourceManager resourceManager, ResourceLocation atlasLocation) {
        List<SpriteSource> outSources = new ArrayList<>(sources.size());
        for (SpriteSource source : sources) {
            if (source instanceof SpriteProvider.Wrapper<?> wrapper) {
                outSources.add(wrapper.withLocation(atlasLocation));
            } else {
                outSources.add(source);
            }
        }
        return outSources;
    }
}
