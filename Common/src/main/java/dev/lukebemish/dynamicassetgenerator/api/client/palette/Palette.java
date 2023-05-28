package dev.lukebemish.dynamicassetgenerator.api.client.palette;

import com.mojang.datafixers.util.Pair;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A collection of RGB24 colors sorted from lightest to darkest, with a cutoff for fuzzy equality. Can be "extended" to
 * a given range by creating new colors at the endpoints as needed. Provides transforms between palette colors and
 * "sample numbers", which are integers in the range [0, 255] corresponding to the position of a color in the palette.
 */
@SuppressWarnings("unused")
public class Palette implements Collection<Integer> {
    private final Set<Integer> backing;
    private List<Integer> colors;

    private final double cutoff;

    private int extendedLow = 0;
    private int extendedHigh = 0;

    /**
     * Creates a new palette with the given cutoff.
     * @param cutoff cutoff for fuzzy equality
     */
    public Palette(double cutoff) {
        this.cutoff = cutoff;
        this.backing = new FuzzySet<>((i1, i2) -> ColorTools.RGB24.distance(i1, i2) < cutoff, colors -> {
            int r = 0;
            int g = 0;
            int b = 0;
            for (Integer color : colors) {
                r += FastColor.ARGB32.red(color);
                g += FastColor.ARGB32.green(color);
                b += FastColor.ARGB32.blue(color);
            }
            return FastColor.ARGB32.color(0xFF, r / colors.size(), g / colors.size(), b / colors.size());
        });
        updateList();
    }

    /**
     * @return the cutoff for fuzzy equality
     */
    public double getCutoff() {
        return cutoff;
    }

    @Override
    public boolean add(Integer color) {
        return add((int) color);
    }

    public boolean add(int color) {
        color = color | 0xFF000000;
        boolean mutated = backing.add(color);
        if (mutated)
            updateList();
        return mutated;
    }

    @Override
    public boolean addAll(Collection<? extends Integer> colors) {
        boolean mutated = backing.addAll(colors.stream().map(i -> i | 0xFF000000).toList());
        if (mutated)
            updateList();
        return mutated;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection) {
        boolean mutated = backing.removeAll(collection);
        if (mutated)
            updateList();
        return mutated;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection) {
        boolean mutated = backing.retainAll(collection);
        if (mutated)
            updateList();
        return mutated;
    }

    @Override
    public void clear() {
        backing.clear();
        updateList();
    }

    /**
     * Extends the high and low ends of the palette until it stretches to a given size.
     * @param targetWidth the target Euclidean distance between the lowest and highest colors in the palette
     * @throws IllegalStateException if the palette is empty
     */
    public void extend(Integer targetWidth) {
        if (backing.isEmpty())
            throw new IllegalStateException("Color palette is empty");
        // TODO implement...
    }

    /**
     * @return the number of colors in the palette before it was extended
     */
    public int originalSize() {
        return backing.size() - extendedLow - extendedHigh;
    }

    @Override
    public int size() {
        return colors.size();
    }

    @Override
    public boolean isEmpty() {
        return colors.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return backing.contains(o);
    }

    @NotNull
    @Override
    public Iterator<Integer> iterator() {
        return colors.iterator();
    }

    @Override
    public Object @NotNull [] toArray() {
        return colors.toArray();
    }

    @Override
    public <T> T @NotNull [] toArray(T @NotNull [] ts) {
        return colors.toArray(ts);
    }

    @Override
    public boolean remove(Object o) {
        boolean mutated = backing.remove(o);
        if (mutated)
            updateList();
        return mutated;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection) {
        return backing.containsAll(collection);
    }

    /**
     * @return the index of the first color in the palette before it was extended
     * @throws IllegalStateException if the palette is empty
     */
    public int originalStart() {
        if (backing.isEmpty())
            throw new IllegalStateException("Color palette is empty");
        return extendedLow;
    }

    /**
     * @return the index of the last color in the palette before it was extended
     * @throws IllegalStateException if the palette is empty
     */
    public int originalEnd() {
        if (backing.isEmpty())
            throw new IllegalStateException("Color palette is empty");
        return colors.size() - extendedHigh - 1;
    }

    /**
     * @return a sample number pointing towards the center of the palette before it was extended
     * @throws IllegalStateException if the palette is empty
     */
    public int originalCenterSample() {
        return (originalStart() + originalEnd()) * 255 / colors.size() / 2;
    }

    /**
     * @return a sample number pointing towards the start of the palette before it was extended
     * @throws IllegalStateException if the palette is empty
     */
    public int originalStartSample() {
        return originalStart() * 255 / colors.size();
    }

    /**
     * @return a sample number pointing towards the end of the palette before it was extended
     * @throws IllegalStateException if the palette is empty
     */
    public int originalEndSample() {
        return originalEnd() * 255 / colors.size();
    }

    /**
     * @return a sample number in the extended palette pointing towards the same color that the provided sample number
     * would have before the palette was extended
     * @throws IllegalStateException if the palette is empty
     */
    public int originalToExtended(int originalSample) {
        if (backing.isEmpty()) {
            throw new IllegalStateException("Color palette is empty");
        }
        var originalSampleSize = originalSize() * 255 / colors.size();
        return originalSample * originalSampleSize / 255 + originalStartSample();
    }

    /**
     * @return a sample number in the original palette pointing towards the same color that the provided sample number
     * would have in the extended palette
     * @throws IllegalStateException if the palette is empty
     */
    public int extendedToOriginal(int extendedSample) {
        if (originalSize() == 0) {
            throw new IllegalStateException("Original color palette was empty");
        }
        var originalSampleSize = originalSize() * 255 / colors.size();
        var output = (extendedSample - originalStartSample()) * 255 / originalSampleSize;
        return ColorTools.clamp8(output);
    }

    /**
     * @return a sample number pointing towards where the provided color would lie in the palette
     */
    public int getSample(int color) {
        List<Pair<Integer, Double>> indexWithDistance = new ArrayList<>();
        for (int i = 0; i < colors.size(); i++) {
            indexWithDistance.add(Pair.of(i, ColorTools.RGB24.distance(color, colors.get(i))));
        }
        indexWithDistance.sort(Comparator.comparingDouble(Pair::getSecond));
        if (indexWithDistance.isEmpty())
            throw new IllegalStateException("Color palette is empty");
        if (indexWithDistance.size() == 1)
            return indexWithDistance.get(0).getFirst() * 255 / colors.size();
        else {
            var colorMain = indexWithDistance.get(0);
            var colorNext = indexWithDistance.get(1);
            double distance = colorMain.getSecond() + colorNext.getSecond();
            double lerp = Math.max(0, Math.min(1, colorMain.getSecond() / distance));
            return (int) Math.round((colorMain.getFirst() * (1 - lerp) + colorNext.getFirst() * lerp) * 255 / colors.size());
        }
    }

    /**
     * @return the color in the palette closes to the given sample number
     */
    public int getColor(int sample) {
        if (sample < 0 || sample >= colors.size())
            return -1;
        return colors.get(sample);
    }

    private void updateList() {
        colors = backing.stream().sorted(ColorTools.RGB24.COMPARATOR).toList();
    }
}
