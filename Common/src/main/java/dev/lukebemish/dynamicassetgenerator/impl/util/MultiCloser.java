/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.util;

import dev.lukebemish.dynamicassetgenerator.impl.DynamicAssetGenerator;

import java.util.Collection;

public class MultiCloser implements AutoCloseable {
    private final Collection<? extends AutoCloseable> toClose;
    public MultiCloser(Collection<? extends AutoCloseable> toClose) {
        this.toClose = toClose;
    }

    @Override
    public void close() {
        for (AutoCloseable c : toClose) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                    DynamicAssetGenerator.LOGGER.debug("Exception while closing resources:\n", e);
                }
            }
        }
    }
}
