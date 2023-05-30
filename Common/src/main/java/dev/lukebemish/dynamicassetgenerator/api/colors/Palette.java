package dev.lukebemish.dynamicassetgenerator.api.colors;

import com.mojang.datafixers.util.Pair;
import dev.lukebemish.dynamicassetgenerator.api.util.FuzzySet;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

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
        this.backing = new FuzzySet<>((i1, i2) -> ColorTools.ARGB32.distance(i1, i2) < cutoff, colors -> {
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
     * @return a new palette with the given cutoff and colors
     */
    public static Palette fromColors(Collection<Integer> colors, double cutoff) {
        Palette palette = new Palette(cutoff);
        palette.addAll(colors);
        return palette;
    }

    /**
     * @return a new palette with the given cutoff and colors from the provided palette
     */
    public static Palette fromPalette(Palette palette, double cutoff) {
        Palette newPalette = new Palette(cutoff);
        newPalette.addAll(palette);
        return newPalette;
    }

    /**
     * @return a new palette with colors and cutoff copied from the provided palette
     */
    public static Palette fromPalette(Palette palette) {
        return fromPalette(palette, palette.getCutoff());
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

    private static final int MAX_WIDTH = (int) Math.floor(ColorTools.ARGB32.distance(0x000000, 0xFFFFFF));

    /**
     * Extends the high and low ends of the palette until it stretches to a given size.
     * @param targetWidth the target Euclidean distance between the lowest and highest colors in the palette
     * @throws IllegalStateException if the palette is empty
     */
    public void extendToWidth(int targetWidth) {
        this.extend(palette -> ColorTools.ARGB32.distance(palette.colors.get(0), palette.colors.get(palette.colors.size() - 1)) >= targetWidth);
    }

    /**
     * Extends the high and low ends of the palette until it contains a given number of colors.
     * @param targetSize the target number of colors in the palette
     * @throws IllegalStateException if the palette is empty
     */
    public void extendToSize(int targetSize) {
        this.extend(palette -> palette.size() >= targetSize);
    }

    /**
     * Extends the high and low ends of the palette until it satisfies a given predicate. Extension may be impossible if
     * the palette's entries are too close together and the cutoff for equivalent colors is too broad. If the predicate
     * is never satisfied, extension will stop when 0xFFFFFF and 0x000000 are reached.
     * @param isExtended predicate to test whether the palette is extended enough
     * @throws IllegalStateException if the palette is empty
     */
    public void extend(Predicate<Palette> isExtended) {
        if (backing.isEmpty())
            throw new IllegalStateException("Color palette is empty");
        boolean isLow = true;
        boolean reachedEndpoint = false;
        double spacing = ColorTools.ARGB32.distance(colors.get(0), colors.get(colors.size() - 1)) / (colors.size() - 1);
        while (!isExtended.test(this)) {
            int end = isLow ? colors.get(0) : colors.get(colors.size() - 1);
            double endDistance = ColorTools.ARGB32.distance(end, isLow ? 0x000000 : 0xFFFFFF);
            int oldSize = backing.size();
            if (endDistance < spacing) {
                int newColor = isLow ? 0x000000 : 0xFFFFFF;
                backing.add(newColor);
                reachedEndpoint = !updateExtended(isLow, oldSize);
                if (!reachedEndpoint)
                    updateList();
                if (reachedEndpoint)
                    break;
                isLow = !isLow;
                reachedEndpoint = true;
            }

            float lerp = (float) (spacing / endDistance);
            int newColor = FastColor.ARGB32.lerp(lerp, end, isLow ? 0xFF000000 : 0xFFFFFFFF);
            backing.add(newColor);
            boolean didExtension = updateExtended(isLow, oldSize);
            if (didExtension)
                updateList();
            if (!didExtension) {
                if (reachedEndpoint) {
                    break;
                } else {
                    isLow = !isLow;
                    reachedEndpoint = true;
                }
            }

            if (!reachedEndpoint)
                isLow = !isLow;
        }
    }

    private boolean updateExtended(boolean isLow, int oldSize) {
        if (oldSize == backing.size())
            return false;
        if (isLow)
            extendedLow++;
        else
            extendedHigh++;
        return true;
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
        color = color & 0xFF000000;
        if (colors.isEmpty())
            throw new IllegalStateException("Color palette is empty");
        List<Pair<Integer, Double>> indexWithDistance = new ArrayList<>();
        for (int i = 0; i < colors.size(); i++) {
            indexWithDistance.add(Pair.of(i, ColorTools.ARGB32.distance(color, colors.get(i))));
        }
        indexWithDistance.sort(Comparator.comparingDouble(Pair::getSecond));
        if (indexWithDistance.size() == 1 || indexWithDistance.get(0).getSecond() <= this.cutoff)
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
     * @throws IllegalArgumentException if the sample number is not between 0 and 255
     */
    public int getColor(int sample) {
        if (sample < 0 || sample > 255)
            throw new IllegalArgumentException("Sample number must be between 0 and 255");
        return colors.get(sample * colors.size() / 255);
    }

    /**
     * @return the color in the palette at the given index
     */
    public int getColorFromIndex(int index) {
        return colors.get(index);
    }

    /**
     * @return the color in the palette closest to the given color
     */
    public int getClosestColor(int color) {
        if (colors.isEmpty())
            throw new IllegalStateException("Color palette is empty");
        List<Pair<Integer, Double>> indexWithDistance = new ArrayList<>();
        for (int knownColor : colors) {
            indexWithDistance.add(Pair.of(knownColor, ColorTools.ARGB32.distance(color, knownColor)));
        }
        indexWithDistance.sort(Comparator.comparingDouble(Pair::getSecond));
        return indexWithDistance.get(0).getFirst();
    }

    /**
     * @return the average color of the palette
     */
    public int getAverage() {
        int c = 0;
        int r = 0;
        int g = 0;
        int b = 0;
        for (int color : colors) {
            c++;
            r += FastColor.ARGB32.red(color);
            g += FastColor.ARGB32.green(color);
            b += FastColor.ARGB32.blue(color);
        }
        return FastColor.ARGB32.color(0xFF, r / c, g / c, b / c);
    }

    private void updateList() {
        colors = backing.stream().sorted(ColorTools.ARGB32.COMPARATOR).toList();
    }
}
