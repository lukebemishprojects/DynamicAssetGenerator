package mod_template;

import com.google.common.base.Suppliers;
import dev.architectury.registry.registries.Registries;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class ModTemplate {
    public static final String MOD_ID = "mod_template";
    // We can use this if we don't want to use DeferredRegister
    public static final Supplier<Registries> REGISTRIES = Suppliers.memoize(() -> Registries.get(MOD_ID));

    public static void init() {
    }
}
