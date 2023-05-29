/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators.texsources;

import com.mojang.serialization.Codec;
import dev.lukebemish.dynamicassetgenerator.api.client.generators.ITexSource;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.Operations;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;

import java.util.List;

public class OverlaySource extends AbstractManyOperationSource {
    public static final Codec<OverlaySource> CODEC = AbstractManyOperationSource.makeCodec(OverlaySource::new);

    public OverlaySource(List<ITexSource> sources) {
        super(sources);
    }

    @Override
    public Codec<? extends ITexSource> codec() {
        return CODEC;
    }

    @Override
    public PointwiseOperation.ManyPointwiseOperation<Integer> getOperation() {
        return Operations.OVERLAY;
    }
}
