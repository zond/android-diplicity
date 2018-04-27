package se.oort.diplicity.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Map to count occurrences of a type of object.
 */
public class Counter<T> extends HashMap<T, Integer> {
    public void increment(T key) {
        this.put(key, this.get(key) + 1);
    }

    @Override
    public Integer get(Object key) {
        if (!this.containsKey(key)) {
            return 0;
        }
        return super.get(key);
    }
}
