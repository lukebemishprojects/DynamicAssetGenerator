/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.colors;

import com.mojang.serialization.Codec;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.CachedConversionOperation;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.ChannelOperation;
import dev.lukebemish.dynamicassetgenerator.api.colors.operations.PointwiseOperation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Represents a channel of a color, and how a pointwise operator to retrieve that color from an ARGB32 encoded color.
 */
public enum Channel implements StringRepresentable {
    RED(() -> new ChannelOperation(2)),
    GREEN(() -> new ChannelOperation(1)),
    BLUE(() -> new ChannelOperation(0)),
    ALPHA(() -> new ChannelOperation(3)),

    CIELAB_LIGHTNESS(() -> PointwiseOperation.Unary.chain(
            new CachedConversionOperation(new ColorTools.ConversionCache(ColorTools.CIELAB32::fromARGB32)),
            new ChannelOperation(2))
    ),
    CIELAB_A(() -> PointwiseOperation.Unary.chain(
            new CachedConversionOperation(new ColorTools.ConversionCache(ColorTools.CIELAB32::fromARGB32)),
            new ChannelOperation(1))
    ),
    CIELAB_B(() -> PointwiseOperation.Unary.chain(
            new CachedConversionOperation(new ColorTools.ConversionCache(ColorTools.CIELAB32::fromARGB32)),
            new ChannelOperation(0))
    ),

    HSL_LIGHTNESS(() -> PointwiseOperation.Unary.chain(
            new CachedConversionOperation(new ColorTools.ConversionCache(ColorTools.HSL32::fromARGB32)),
            new ChannelOperation(2))
    ),
    HSL_SATURATION(() -> PointwiseOperation.Unary.chain(
            new CachedConversionOperation(new ColorTools.ConversionCache(ColorTools.HSL32::fromARGB32)),
            new ChannelOperation(1))
    ),
    HSL_HUE(() -> PointwiseOperation.Unary.chain(
            new CachedConversionOperation(new ColorTools.ConversionCache(ColorTools.HSL32::fromARGB32)),
            new ChannelOperation(0))
    );

    private final Supplier<PointwiseOperation.Unary<Integer>> operation;

    Channel(Supplier<PointwiseOperation.Unary<Integer>> operation) {
        this.operation = operation;
    }

    /**
     * @return a new operation, with a fresh cache, that extracts this channel from an ARGB32 color.
     */
    public PointwiseOperation.Unary<Integer> makeOperation() {
        return operation.get();
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static final Codec<Channel> CODEC = StringRepresentable.fromEnum(Channel::values);
}
