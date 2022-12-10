/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiCloser implements AutoCloseable {
    private final Collection<? extends AutoCloseable> toClose;
    public MultiCloser(Collection<? extends AutoCloseable> toClose) {
        this.toClose = toClose;
    }

    @Override
    public void close() throws Exception {
        List<Exception> exceptions = new ArrayList<>();
        for (AutoCloseable c : toClose) {
            try {
                c.close();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            if (exceptions.stream().anyMatch(it -> it instanceof RuntimeException))
                throw new MultiCloseRuntimeException(exceptions);
            throw new MultiCloseException(exceptions);
        }
    }

    public static class MultiCloseException extends Exception {
        public MultiCloseException(List<Exception> exceptions) {
            super("Multiple exceptions occurred while closing", exceptions.get(0));
            exceptions.forEach(this::addSuppressed);
        }
    }

    public static class MultiCloseRuntimeException extends RuntimeException {
        public MultiCloseRuntimeException(List<Exception> exceptions) {
            super("Multiple exceptions occurred while closing", exceptions.get(0));
            exceptions.forEach(this::addSuppressed);
        }
    }
}
