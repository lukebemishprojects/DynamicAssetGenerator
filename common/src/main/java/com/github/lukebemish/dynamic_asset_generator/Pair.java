package com.github.lukebemish.dynamic_asset_generator;

import java.util.Objects;

public record Pair<F,L>(F first, L last) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(first, pair.first) && Objects.equals(last, pair.last);
    }

    @Override
    public String toString() {
        return "Pair{" +
                "first=" + first +
                ", last=" + last +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, last);
    }
}
