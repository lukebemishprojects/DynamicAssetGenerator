package io.github.lukebemish.dynamic_asset_generator.impl.util;

import io.github.lukebemish.dynamic_asset_generator.impl.DynamicAssetGenerator;

import java.util.Collection;

public class MultiCloser implements AutoCloseable {
    private final Collection<? extends AutoCloseable> toClose;
    public MultiCloser(Collection<? extends AutoCloseable> toClose) {
        this.toClose = toClose;
    }

    @Override
    public void close() {
        for (AutoCloseable c : toClose) {
            try {
                c.close();
            } catch (Exception e) {
                DynamicAssetGenerator.LOGGER.error("Exception while closing resources:\n",e);
            }
        }
    }
}
