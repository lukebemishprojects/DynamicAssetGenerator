package com.github.lukebemish.dynamic_asset_generator.client.util;

import com.github.lukebemish.dynamic_asset_generator.client.palette.ColorHolder;
import com.github.lukebemish.dynamic_asset_generator.client.palette.Palette;
import com.mojang.math.Vector3f;

import java.util.*;

public class Clusterer {
    Map<Vector3f,Integer> assignmentMap = new HashMap<>();
    List<Cluster> sampleList = new ArrayList<>();
    double cutoff = -1; //won't merge above this

    private void run() {
        int size = sampleList.size();
        while(size > 2) {
            int xF = 0;
            int yF = 1;
            double minNon0Dist = sampleList.get(0).dist(sampleList.get(1));
            for (int x = 0; x < sampleList.size(); x++) {
                for (int y = 0; y < sampleList.size(); y++) {
                    if (x!=y) {
                        double d = sampleList.get(x).dist(sampleList.get(y));
                        if (d < minNon0Dist) minNon0Dist = d;
                    }
                }
            }
            if (cutoff>0 && minNon0Dist>cutoff ) {
                break;
            }
            Cluster toAdd = sampleList.get(xF);
            toAdd.merge(sampleList.remove(yF));
            size = sampleList.size();
        }
        int i = 0;
        for (Cluster c: sampleList) {
            for (Vector3f v: c) {
                assignmentMap.put(v,i);
            }
            i++;
        }
    }
    public boolean isInCategoryWith(Vector3f a, Vector3f b) {
        return assignmentMap.getOrDefault(a, -1).equals(assignmentMap.getOrDefault(b,-2));
    }

    public int getCategory(Vector3f v) {
        return assignmentMap.getOrDefault(v, -1);
    }

    public static Clusterer createFromPalettes(Palette bg, Palette... ps) {
        Clusterer out = new Clusterer();
        Set<Vector3f> samples = new HashSet<>();
        for (Palette p : ps) {
            p.getStream().forEach(c->{
                samples.add(c.toCIELAB().toVector3f());
            });
        }
        Cluster bgCluster = new Cluster();
        for (ColorHolder c : bg.getStream().toList()) {
            Vector3f v = c.toCIELAB().toVector3f();
            samples.remove(v);
            bgCluster.add(v);
        }
        out.sampleList.add(bgCluster);
        for (Vector3f v : samples) {
            out.sampleList.add(Cluster.single(v));
        }
        out.cutoff = bgCluster.minDist();
        out.run();
        return out;
    }

    public static class Cluster extends ArrayList<Vector3f> {
        public double dist(Cluster other) {
            double min = get(0).dot(other.get(0));
            for (Vector3f f : this) {
                for (Vector3f vector3f : other) {
                    double d = f.dot(vector3f);
                    if (d < min) min = d;
                }
            }
            return min;
        }
        public static Cluster single(Vector3f v) {
            Cluster c = new Cluster();
            c.add(v);
            return c;
        }
        public void merge(Cluster cluster) {
            this.addAll(cluster);
        }
        public double minDist() {
            double m = get(0).dot(get(0));
            for (Vector3f x : this) {
                for (Vector3f y : this) {
                    double d = x.dot(y);
                    if (d<m) m=d;
                }
            }
            return m;
        }
        public double maxDist() {
            double m = get(0).dot(get(0));
            for (Vector3f x : this) {
                for (Vector3f y : this) {
                    double d = x.dot(y);
                    if (d>m) m=d;
                }
            }
            return m;
        }
        public double avgDist() {
            double m = 0;
            int c = 0;
            for (Vector3f x : this) {
                for (Vector3f y : this) {
                    if (!x.equals(y)) {
                        m += x.dot(y);
                        c++;
                    }
                }
            }
            return m/c;
        }
    }
}
