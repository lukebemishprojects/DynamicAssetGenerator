package dynamic_asset_generator.api;

import java.util.function.Supplier;

public interface ResettingSupplier extends Supplier {
    public void reset();
}
