/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.colors.clustering;

import dev.lukebemish.dynamicassetgenerator.api.colors.ColorTypes;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;

import java.util.Collection;

/**
 * A custer of colors, to be used with a {@link Clusterer}.
 */
public class Cluster {
    private final IntList colors = new IntArrayList();

    /**
     * Create a cluster containing only the provided color.
     */
    @SuppressWarnings("unused")
    public Cluster(int color) {
        colors.add(color);
    }

    /**
     * Create a cluster containing the provided colors
     * @param colors the colors to add to the cluster. Must be non-empty
     * @throws IllegalArgumentException if no colors are provided
     */
    @SuppressWarnings("unused")
    public Cluster(Collection<Integer> colors) {
        if (colors.isEmpty())
            throw new IllegalArgumentException("Attempted to create a cluster with no colors");
        this.colors.addAll(colors);
    }

    /**
     * Calculate the distance between this cluster and another cluster.
     * @param other the other cluster to compare to
     * @param rgb2labCache a cache to use for converting colors to CIELAB, linked to the lifespan of the parent clusterer
     * @return the distance between the two clusters in CIELAB32 space
     */
    public double dist(Cluster other, ColorTypes.ConversionCache32 rgb2labCache) {
        double min = ColorTypes.CIELAB32.distance(rgb2labCache.convert(colors.getInt(0)), rgb2labCache.convert(other.colors.getInt(0)));
        for (int f : colors) {
            for (int c : other.colors) {
                double d = ColorTypes.CIELAB32.distance(rgb2labCache.convert(f), rgb2labCache.convert(c));
                if (d < min)
                    min = d;
            }
        }
        return min;
    }

    /**
     * Merges the provided cluster into this cluster.
     */
    public void merge(Cluster other) {
        colors.addAll(other.colors);
    }

    /**
     * @return an unmodifiable view of the colors in this cluster
     */
    public IntList getColors() {
        return IntLists.unmodifiable(colors);
    }
}
