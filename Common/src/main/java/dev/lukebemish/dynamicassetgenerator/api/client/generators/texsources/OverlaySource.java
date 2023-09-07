/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.serialization.Codec;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.ColorOperations;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * A {@link AbstractManyOperationSource} defined by {@link ColorOperations#OVERLAY}.
 */
public class OverlaySource extends AbstractManyOperationSource {
    public static final Codec<OverlaySource> CODEC = AbstractManyOperationSource.makeCodec(OverlaySource::new);

    private OverlaySource(List<TexSource> sources) {
        super(sources);
    }

    @Override
    public @NotNull Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public PointwiseOperation.Any<Integer> getOperation() {
        return ColorOperations.OVERLAY;
    }

    public static class Builder {
        private List<TexSource> sources;

        /**
         * Sets the sources to overlay, from highest to lowest.
         */
        public Builder setSources(List<TexSource> sources) {
            this.sources = sources;
            return this;
        }

        public OverlaySource build() {
            Objects.requireNonNull(sources);
            return new OverlaySource(sources);
        }
    }
}
