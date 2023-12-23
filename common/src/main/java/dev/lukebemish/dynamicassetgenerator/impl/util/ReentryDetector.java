/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.util;

import java.util.HashSet;
import java.util.Set;

public class ReentryDetector<T> {
    private final ThreadLocal<Set<T>> threadLocal = ThreadLocal.withInitial(HashSet::new);

    public Lock reentrant(T value) {
        var reentrant = !threadLocal.get().add(value);
        return new Lock(reentrant);
    }

    public class Lock implements AutoCloseable {
        private final boolean reentrant;

        public Lock(boolean reentrant) {
            this.reentrant = reentrant;
        }

        public boolean reentrant() {
            return reentrant;
        }

        @Override
        public void close(){
            threadLocal.get().clear();
        }
    }
}
