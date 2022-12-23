/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl;

import java.util.function.Consumer;
import java.util.function.Function;

public class CacheReference<T> {
    T held = null;

    public T calcSync(Function<T, T> transformer) {
        synchronized (this) {
            return transformer.apply(held);
        }
    }

    public void doSync(Consumer<T> consumer) {
        synchronized (this) {
            consumer.accept(held);
        }
    }

    public T getHeld() {
        return held;
    }

    public void setHeld(T held) {
        synchronized (this) {
            this.held = held;
        }
    }
}
