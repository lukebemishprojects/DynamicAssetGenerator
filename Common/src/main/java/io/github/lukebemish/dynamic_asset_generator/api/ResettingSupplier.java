package io.github.lukebemish.dynamic_asset_generator.api;

import java.util.function.Supplier;

public interface ResettingSupplier<T> extends Supplier<T> {
    void reset();
    static <R> ResettingSupplier<R> of(Supplier<R> s, Runnable r) {
        return new ResettingSupplier<R>() {
            @Override
            public void reset() {
                r.run();
            }

            @Override
            public R get() {
                return s.get();
            }
        };
    }
    static <R> ResettingSupplier<R> of(Supplier<R> s,Object creator) {
        if (creator instanceof ResettingSupplier<?> rs) {
            return of(s, rs::reset);
        }
        return of(s,()->{});
    }
}
