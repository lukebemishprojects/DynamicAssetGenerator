/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.dynamicassetgenerator.api.util;

import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * A set that allows for fuzzy matching of elements.
 */
public class FuzzySet<T> implements Set<T> {
    private final Map<T,Set<T>> fuzzy = new HashMap<>();
    private final BiPredicate<T,T> cutoff;
    private final Function<Collection<T>, T> averager;

    /**
     * Creates a new fuzzy set with the given cutoff and averager.
     * @param cutoff determines whether two elements are close enough to consider them equivalent
     * @param averager determines the "average" of a set of elements
     */
    public FuzzySet(BiPredicate<T,T> cutoff, Function<Collection<T>, T> averager) {
        this.cutoff = cutoff;
        this.averager = averager;
    }

    @Override
    public int size() {
        return fuzzy.size();
    }

    @Override
    public boolean isEmpty() {
        return fuzzy.isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        T t = (T) o;
        outer:
        for (T key : fuzzy.keySet()) {
            if (cutoff.test(key, t)) {
                Set<T> set = fuzzy.get(key);
                if (set.size() != 1) {
                    for (T e : set) {
                        if (!cutoff.test(e, t)) {
                            continue outer;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return fuzzy.keySet().iterator();
    }

    @Override
    public Object @NonNull [] toArray() {
        return fuzzy.keySet().toArray();
    }

    @Override
    public <T1> T1 @NonNull [] toArray(T1 @NonNull [] t1s) {
        return fuzzy.keySet().toArray(t1s);
    }

    @Override
    public boolean add(T t) {
        outer:
        for (T key : fuzzy.keySet()) {
            if (cutoff.test(key, t)) {
                Set<T> set = fuzzy.get(key);
                if (set.size() != 1) {
                    for (T e : set) {
                        if (!cutoff.test(e, t)) {
                            continue outer;
                        }
                    }
                }
                if (set.contains(t))
                    return false;
                set.add(t);
                T newKey = averager.apply(set);
                fuzzy.remove(key);
                fuzzy.put(newKey, set);
                return false;
            }
        }
        fuzzy.put(t, new HashSet<>());
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
        for (T key : fuzzy.keySet()) {
            if (cutoff.test(key, (T) o)) {
                fuzzy.remove(key);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> collection) {
        for (Object o : collection) {
            if (!contains(o))
                return false;
        }
        return true;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> collection) {
        boolean changed = false;
        for (T t : collection) {
            changed |= add(t);
        }
        return changed;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean retainAll(@NonNull Collection<?> collection) {
        var keys = fuzzy.keySet().stream().filter(ts -> {
            for (Object o : collection) {
                if (cutoff.test(ts, (T) o))
                    return false;
            }
            return true;
        }).iterator();

        boolean changed = false;
        while (keys.hasNext()) {
            T key = keys.next();
            fuzzy.remove(key);
            changed = true;
        }
        return changed;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean removeAll(@NonNull Collection<?> collection) {
        var keys = fuzzy.keySet().stream().filter(ts -> {
            for (Object o : collection) {
                if (cutoff.test(ts, (T) o))
                    return true;
            }
            return false;
        }).iterator();

        boolean changed = false;
        while (keys.hasNext()) {
            T key = keys.next();
            fuzzy.remove(key);
            changed = true;
        }
        return changed;
    }

    @Override
    public void clear() {
        fuzzy.clear();
    }
}
