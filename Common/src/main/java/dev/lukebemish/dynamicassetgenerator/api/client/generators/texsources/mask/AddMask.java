/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.mask;

import com.mojang.serialization.Codec;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.AbstractManyOperationSource;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.ColorOperations;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;

import java.util.List;
import java.util.Objects;

/**
 * An {@link AbstractManyOperationSource} defined by {@link ColorOperations#ADD}.
 */
public class AddMask extends AbstractManyOperationSource {
    public static final Codec<AddMask> CODEC = AbstractManyOperationSource.makeCodec(AddMask::new);


    private AddMask(List<TexSource> sources) {
        super(sources);
    }

    @Override
    public Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public PointwiseOperation.Any<Integer> getOperation() {
        return ColorOperations.ADD;
    }

    public static class Builder {
        private List<TexSource> sources;

        /**
         * Sets the sources whose values to add.
         */
        public Builder setSources(List<TexSource> sources) {
            this.sources = sources;
            return this;
        }

        public AddMask build() {
            Objects.requireNonNull(sources);
            return new AddMask(sources);
        }
    }
}
