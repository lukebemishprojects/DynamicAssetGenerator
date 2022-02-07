package dynamic_asset_generator.api;

import java.util.function.Supplier;

public interface ResettingSupplier<T> extends Supplier<T> {
    public void reset();
}
