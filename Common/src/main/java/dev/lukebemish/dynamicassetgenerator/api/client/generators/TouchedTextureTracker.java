/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.client.generators;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class TouchedTextureTracker {
    private final List<ResourceLocation> touchedTextures = new ArrayList<>();
    private final Set<ResourceLocation> touchedTexturesSet = new HashSet<>();
    private final List<ResourceLocation> view = Collections.unmodifiableList(touchedTextures);

    /**
     * @param texture the location of a texture, without the leading {@code "textures/"} prefix or file extension, that
     *                the texture source has used
     */
    public void addTouchedTexture(ResourceLocation texture) {
        if (touchedTexturesSet.contains(texture)) {
            return;
        }
        touchedTexturesSet.add(texture);
        touchedTextures.add(texture);
    }

    /**
     * @return the list of textures that have been touched by texture sources seeing this
     */
    public List<ResourceLocation> getTouchedTextures() {
        return view;
    }
}
