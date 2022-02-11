package dynamic_asset_generator;

import dynamic_asset_generator.api.ResettingSupplier;

import java.io.InputStream;
import java.nio.file.Path;

public class FileReader implements ResettingSupplier<InputStream> {
    private final Path path;

    public FileReader(Path path) {
        this.path = path;
    }

    @Override
    public void reset() {

    }

    @Override
    public InputStream get() {
        return null;
    }
}
