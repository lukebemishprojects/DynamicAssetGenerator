/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.mask;

import com.mojang.serialization.Codec;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.TexSource;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources.AbstractManyOperationSource;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.Operations;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;

import java.util.List;

public class AddMask extends AbstractManyOperationSource {
    public static final Codec<AddMask> CODEC = AbstractManyOperationSource.makeCodec(AddMask::new);

    public AddMask(List<TexSource> sources) {
        super(sources);
    }

    @Override
    public Codec<? extends TexSource> codec() {
        return CODEC;
    }

    @Override
    public PointwiseOperation.Any<Integer> getOperation() {
        return Operations.ADD;
    }
}
