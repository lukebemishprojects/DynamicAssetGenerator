package palette_extractor.mixin;

import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(PackRepository.class)
public interface IPackRepositoryMixin {
    @Accessor
    public List<Pack> getSelected();
}
