package com.github.lukebemish.dynamic_asset_generator;

import com.github.lukebemish.dynamic_asset_generator.api.ResettingSupplier;

import java.util.function.Supplier;

public class WrappedSupplier<T> implements ResettingSupplier<T> {
    private final Supplier<T> s;

    public WrappedSupplier(Supplier<T> s) {
        this.s = s;
    }

    @Override
    public T get() {
        return s.get();
    }

    @Override
    public void reset() {
        if (s instanceof ResettingSupplier<T> sr) sr.reset();
    }
}
