package dev.lukebemish.dynamicassetgenerator.api.colors.clustering;

import dev.lukebemish.dynamicassetgenerator.api.colors.ColorTools;

import java.util.*;

/**
 * A tool for grouping colors by agglomerative clustering. Groups are represented by {@link Cluster}s.
 */
@SuppressWarnings("unused")
public class Clusterer {
    private final Map<Integer, Integer> colorToClusterIndex = new HashMap<>();
    private final List<Cluster> clusters = new ArrayList<>();
    private final ColorTools.ConversionCache rgb2labCache = new ColorTools.ConversionCache(ColorTools.CIELAB32::fromARGB32);

    private final double cutoff;

    /**
     * Creates a new palette clusterer with the given parameters.
     * @param cutoff if two clusters are within this distance, they will be merged, Use negative values to represent
     *               any distance.
     */
    public Clusterer(double cutoff) {
        this.cutoff = cutoff;
    }

    /**
     * Adds the provided cluster to this clusterer.
     */
    public void addCluster(Cluster cluster) {
        clusters.add(cluster);
        for (int color : cluster.getColors()) {
            colorToClusterIndex.put(color, clusters.size() - 1);
        }
    }

    /**
     * Until there are no more clusters to merge, merges the closest two clusters whose minimum distance is below the
     * cutoff.
     */
    public void run() {
        int size = clusters.size();
        while(size > 2) {
            int xF = 0;
            int yF = 1;
            double minNon0Dist = clusters.get(0).dist(clusters.get(1),rgb2labCache);
            for (int x = 0; x < clusters.size(); x++) {
                for (int y = 0; y < clusters.size(); y++) {
                    if (x!=y) {
                        double d = clusters.get(x).dist(clusters.get(y),rgb2labCache);
                        if (d < minNon0Dist) {
                            minNon0Dist = d;
                            xF=x;
                            yF=y;
                        }
                    }
                }
            }
            if (cutoff<0 || minNon0Dist<cutoff ) {
                Cluster toAdd = clusters.get(xF);
                toAdd.merge(clusters.remove(yF));
            } else {
                break;
            }
            size = clusters.size();
        }
        int i = 0;
        for (Cluster c: clusters) {
            for (int v: c.getColors()) {
                colorToClusterIndex.put(v,i);
            }
            i++;
        }
    }

    /**
     * @return the number of clusters present in the clusterer
     */
    public int clusterCount() {
        return clusters.size();
    }

    /**
     * @return whether the two provided colors are in the same cluster. If neither are present, returns {@code false}
     */
    public boolean areCategoriesEquivalent(int colorA, int colorB) {
        int categoryA = colorToClusterIndex.getOrDefault(colorA, -1);
        int categoryB = colorToClusterIndex.getOrDefault(colorB, -1);
        if (categoryB == -1 || categoryA == -1)
            return false;
        return categoryA == categoryB;
    }

    /**
     * @return an integer index identifying the cluster the provided color can be found in, or {@code -1} if it is not
     * in any cluster
     */
    public int getCategory(int v) {
        return colorToClusterIndex.getOrDefault(v, -1);
    }

    private static double distanceToLab(int colorA, int colorB) {
        int labA = ColorTools.CIELAB32.fromARGB32(colorA);
        int labB = ColorTools.CIELAB32.fromARGB32(colorB);
        return ColorTools.CIELAB32.distance(labA, labB);
    }

    /**
     * @return the minimum CIELAB distance between any two RGB colors in the provided collection
     */
    public static double minimumSpacing(Collection<Integer> integers) {
        if (integers.size() <= 1) return 0;
        double m = -1;
        for (int x : integers) {
            for (int y : integers) {
                if (x!=y) {
                    double d = distanceToLab(x, y);
                    if (d < m || m < 0) m = d;
                }
            }
        }
        return m;
    }

    /**
     * @return the maximum CIELAB distance between any two RGB colors in the provided collection
     */
    public static double maximumSpacing(Collection<Integer> integers) {
        if (integers.size() <= 1) return 0;
        double m = -1;
        for (int x : integers) {
            for (int y : integers) {
                if (x!=y) {
                    double d = distanceToLab(x, y);
                    if (d > m) m = d;
                }
            }
        }
        return m;
    }

    /**
     * @return the average CIELAB distance between any two RGB colors in the provided collection
     */
    public static double averageSpacing(Collection<Integer> integers) {
        if (integers.size() <= 1) return 0;
        double m = 0;
        double c = 0;
        for (int x : integers) {
            for (int y : integers) {
                if (x!=y) {
                    m += distanceToLab(x, y);
                    c++;
                }
            }
        }
        return m/c;
    }
}
