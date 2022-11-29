/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.impl.client.util;

import dev.lukebemish.dynamicassetgenerator.impl.client.palette.ColorHolder;
import dev.lukebemish.dynamicassetgenerator.impl.client.palette.Palette;

import java.util.*;
import java.util.function.Function;

public class Clusterer {
    Map<ColorHolder,Integer> assignmentMap = new HashMap<>();
    List<Cluster> sampleList = new ArrayList<>();
    double cutoff = -1; //won't merge above this
    double distWeight = 0.0;

    private void run() {
        int size = sampleList.size();
        while(size > 2) {
            int xF = 0;
            int yF = 1;
            double minNon0Dist = sampleList.get(0).dist(sampleList.get(1),distWeight);
            for (int x = 0; x < sampleList.size(); x++) {
                for (int y = 0; y < sampleList.size(); y++) {
                    if (x!=y) {
                        double d = sampleList.get(x).dist(sampleList.get(y),distWeight);
                        if (d < minNon0Dist) {
                            minNon0Dist = d;
                            xF=x;
                            yF=y;
                        }
                    }
                }
            }
            if (cutoff<0 || minNon0Dist<cutoff ) {
                Cluster toAdd = sampleList.get(xF);
                toAdd.merge(sampleList.remove(yF));
            } else {
                break;
            }
            size = sampleList.size();
        }
        int i = 0;
        for (Cluster c: sampleList) {
            for (ColorHolder v: c) {
                assignmentMap.put(v,i);
            }
            i++;
        }
    }
    public boolean isInCategoryWith(ColorHolder a, ColorHolder b) {
        return assignmentMap.getOrDefault(a, -1).equals(assignmentMap.getOrDefault(b,-2));
    }

    public int getCategory(ColorHolder v) {
        return assignmentMap.getOrDefault(v, -1);
    }

    public static Clusterer createFromPalettes(double distWeight, Function<Cluster, Double> cutoffFromBackground, Palette bg, Palette... ps) {
        Clusterer out = new Clusterer();
        out.distWeight = distWeight;
        Set<ColorHolder> samples = new HashSet<>();
        for (Palette p : ps) {
            p.getStream().forEach(samples::add);
        }
        Cluster bgCluster = new Cluster();
        for (ColorHolder c : bg.getStream().toList()) {
            samples.remove(c);
            bgCluster.add(c);
        }
        out.sampleList.add(bgCluster);
        for (ColorHolder v : samples) {
            out.sampleList.add(Cluster.single(v));
        }
        out.cutoff = cutoffFromBackground.apply(bgCluster);
        out.run();
        return out;
    }

    public static Clusterer createFromPalettes(Palette bg, Palette... ps) {
        return createFromPalettes(0.0d, c->0d, bg, ps);
    }

    public int numCategories() {
        return sampleList.size();
    }

    public static class Cluster extends ArrayList<ColorHolder> {
        public double dist(Cluster other,double weight) {
            double min = get(0).distanceToLab(other.get(0),0.2f);
            double avg = 0;
            int count = 0;
            for (ColorHolder f : this) {
                for (ColorHolder c : other) {
                    double d = f.distanceToLab(c,0.2f);
                    avg+=d;
                    count+=1;
                    if (d < min) min = d;
                }
            }
            return (min*(1-weight)+(avg/count)*weight);
        }

        public static Cluster single(ColorHolder v) {
            Cluster c = new Cluster();
            c.add(v);
            return c;
        }
        public void merge(Cluster cluster) {
            this.addAll(cluster);
        }
        public double minDist() {
            if (this.size() <= 1) return 0;
            double m = get(0).distanceToLab(get(1));
            for (ColorHolder x : this) {
                for (ColorHolder y : this) {
                    if (x!=y) {
                        double d = x.distanceToLab(y,0.2f);
                        if (d < m) m = d;
                    }
                }
            }
            return m;
        }
        public double maxDist() {
            double m = get(0).distanceToLab(get(0),0.2f);
            for (ColorHolder x : this) {
                for (ColorHolder y : this) {
                    double d = x.distanceToLab(y,0.2f);
                    if (d>m) m=d;
                }
            }
            return m;
        }
        public double avgDist() {
            double m = 0;
            int c = 0;
            for (ColorHolder x : this) {
                for (ColorHolder y : this) {
                    if (!x.equals(y)) {
                        m += x.distanceToLab(y,0.2f);
                        c++;
                    }
                }
            }
            return m/c;
        }
    }
}
