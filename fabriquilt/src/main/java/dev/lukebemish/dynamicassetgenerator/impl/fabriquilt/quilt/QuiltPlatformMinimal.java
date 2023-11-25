package dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.quilt;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;
import dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.fabric.FabricPlatform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackResources;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.stream.Stream;

public class QuiltPlatformMinimal extends FabricPlatform {
    public static final QuiltPlatformMinimal INSTANCE = new QuiltPlatformMinimal();

    private static final boolean isQFAPIPresent = FabricLoader.getInstance().isModLoaded("quilted_fabric_api");
    private static final String GROUP_PACK_CLASS = "org.quiltmc.qsl.resource.loader.api.GroupResourcePack";
    private static final Class<?> GROUP_PACK_RESOURCES;
    private static final MethodHandle GET_GROUP_PACK_PACKS;

    static {
        Class<?> clazz;
        try {
            clazz = FabricPlatform.class.getClassLoader().loadClass(GROUP_PACK_CLASS);
        } catch (ClassNotFoundException e) {
            clazz = null;
        }
        if (clazz == null) {
            GROUP_PACK_RESOURCES = null;
            GET_GROUP_PACK_PACKS = null;
        } else {
            GROUP_PACK_RESOURCES = clazz;
            var lookup = MethodHandles.lookup();
            MethodHandle getter;
            try {
                var privateLookup = MethodHandles.privateLookupIn(clazz, lookup);
                @SuppressWarnings("rawtypes") Class<List> listClazz = List.class;
                getter = privateLookup.findVirtual(clazz, "getPacks", MethodType.methodType(listClazz));
            } catch (IllegalAccessException | NoSuchMethodException e) {
                getter = null;
            }
            GET_GROUP_PACK_PACKS = getter;
        }
        if (GROUP_PACK_RESOURCES == null || GET_GROUP_PACK_PACKS == null && isQFAPIPresent) {
            DynamicAssetGenerator.LOGGER.error("On quilt but could not find quilt class/field to unwrap grouped resources - Dynamic Asset Generator may not work right!");
        }
    }

    private static final boolean[] LOGGED_ERROR = new boolean[1];

    private synchronized void logError(int i) {
        if (!LOGGED_ERROR[i]) {
            if (i == 0) {
                DynamicAssetGenerator.LOGGER.error("On quilt but could not properly use quilt class/field to unwrap grouped resources - Dynamic Asset Generator may not work right!");
            }
            LOGGED_ERROR[i] = true;
        }
    }

    @Override
    public Stream<PackResources> unpackPacks(Stream<? extends PackResources> packs) {
        return packs.flatMap(pack -> {
            if (GROUP_PACK_RESOURCES != null && GET_GROUP_PACK_PACKS != null) {
                if (GROUP_PACK_RESOURCES.isInstance(pack)) {
                    try {
                        @SuppressWarnings({"unchecked", "rawtypes"}) List<? extends PackResources> unpackedPacks =
                            (List<? extends PackResources>) (List) GET_GROUP_PACK_PACKS.invoke(pack);
                        return Stream.concat(Stream.of(pack), unpackPacks(unpackedPacks.stream()));
                    } catch (Throwable e) {
                        logError(0);
                    }
                }
            }
            return super.unpackPacks(Stream.of(pack));
        });
    }
}
