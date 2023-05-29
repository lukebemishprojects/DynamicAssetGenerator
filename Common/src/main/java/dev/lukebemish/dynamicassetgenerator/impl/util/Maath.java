package dev.lukebemish.dynamicassetgenerator.impl.util;

import java.util.ArrayList;
import java.util.List;


public class Maath {
    public static int gcd(List<Integer> ints) {
        if (ints.size() <= 1)
            return ints.get(0);
        if (ints.size() == 2)
            return gcd(ints.get(0),ints.get(1));
        List<Integer> newInts = new ArrayList<>(ints.subList(2,ints.size()));
        newInts.add(0,gcd(ints.get(0),ints.get(1)));
        return gcd(newInts);
    }

    public static int lcm(List<Integer> ints) {
        if (ints.size() <= 1)
            return ints.get(0);
        if (ints.size() == 2)
            return lcm(ints.get(0),ints.get(1));
        List<Integer> newInts = new ArrayList<>(ints.subList(2,ints.size()));
        newInts.add(0,lcm(ints.get(0),ints.get(1)));
        return lcm(newInts);
    }

    public static int gcd(int i1, int i2) {
        if (i1==0||i2==0)
            return i1+i2;
        int max = Math.max(i1,i2);
        int min = Math.min(i1,i2);
        return gcd(max % min, min);
    }

    public static int lcm(int i1, int i2) {
        return (i1*i2)/gcd(i1,i2);
    }
}
