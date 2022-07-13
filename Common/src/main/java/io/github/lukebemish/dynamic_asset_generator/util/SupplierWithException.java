package io.github.lukebemish.dynamic_asset_generator.util;

@FunctionalInterface
public interface SupplierWithException<T,E extends Throwable> {
    T get() throws E;
}