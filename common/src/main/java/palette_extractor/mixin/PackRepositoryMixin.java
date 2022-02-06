package palette_extractor.mixin;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import palette_extractor.PaletteExtractorClientResourcePack;

import java.util.ArrayList;
import java.util.List;

@Mixin(PackRepository.class)
public class PackRepositoryMixin {
    @Inject(method = "openAllSelected", at = @At("RETURN"), cancellable = true)
    private void palette_extractor_insertData(CallbackInfoReturnable<List<PackResources>> ci) {
        ArrayList<PackResources> injected = new ArrayList<>(ci.getReturnValue());
        injected.add(new PaletteExtractorClientResourcePack());
        ci.setReturnValue(injected);
    }
}
