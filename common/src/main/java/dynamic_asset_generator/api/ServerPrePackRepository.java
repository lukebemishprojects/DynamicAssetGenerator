package dynamic_asset_generator.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ServerPrePackRepository {
        //Allows resources to be found while packs are being loaded... not sure how bad of an idea this is.
        private static List<PackResources> resources;

        public static void resetResources() {
            resources = null;
        }

        public static void loadResources(List<PackResources> r) {
            resources = r;
        }

        public static InputStream getResource(ResourceLocation rl) throws IOException {
            InputStream resource = null;
            for (PackResources r : resources) {
                if (r.hasResource(PackType.SERVER_DATA, rl)) {
                    resource = r.getResource(PackType.SERVER_DATA, rl);
                }
            }
            if (resource != null) {
                return resource;
            }
            throw new IOException("Could not find data in pre-load: "+rl.toString());
        }
}