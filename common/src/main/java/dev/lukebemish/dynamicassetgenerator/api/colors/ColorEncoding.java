/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.colors;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.util.function.IntUnaryOperator;

/**
 * Represents different types of color encodings, each with conversions to and from ABGR32, the format used by a
 * {@link com.mojang.blaze3d.platform.NativeImage}. Meant to be used to represent encoded colors in data.
 */
public enum ColorEncoding implements StringRepresentable {
    ARGB("ARGB", ColorTypes.ABGR32::fromARGB32, ColorTypes.ABGR32::toARGB32),
    RGB("RGB", i -> ColorTypes.ABGR32.fromARGB32(i) | 0xFF000000, i -> ColorTypes.ABGR32.toARGB32(i) | 0xFF000000),
    ABGR("ABGR", IntUnaryOperator.identity(), IntUnaryOperator.identity()),
    BGR("BGR", i -> i | 0xFF000000, i -> i | 0xFF000000);

    public final IntUnaryOperator toABGR;
    public final IntUnaryOperator fromABGR;
    private final String name;

    ColorEncoding(String name, IntUnaryOperator toABGR, IntUnaryOperator fromABGR) {
        this.name = name;
        this.toABGR = toABGR;
        this.fromABGR = fromABGR;
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.name;
    }

    /**
     * Codec for {@link ColorEncoding} based on its name.
     */
    public static final Codec<ColorEncoding> CODEC = StringRepresentable.fromEnum(ColorEncoding::values);
}
