package net.neoforged.neoforge.common.util;

import java.util.List;
import java.util.function.Predicate;

/** Matches inputs to ingredients one to one (reamc-compat). */
public class RecipeMatcher {
    /** For each input, the index of the ingredient it fills; null if they don't match. */
    public static <T> int[] findMatches(List<T> inputs, List<? extends Predicate<T>> tests) {
        int n = inputs.size();
        if (n != tests.size()) return null;
        int[] assign = new int[n], owner = new int[n];
        java.util.Arrays.fill(owner, -1);
        for (int i = 0; i < n; i++) if (!augment(i, inputs, tests, owner, new boolean[n])) return null;
        for (int t = 0; t < n; t++) assign[owner[t]] = t;
        return assign;
    }

    private static <T> boolean augment(int i, List<T> inputs, List<? extends Predicate<T>> tests, int[] owner, boolean[] seen) {
        for (int t = 0; t < tests.size(); t++) {
            if (seen[t] || !tests.get(t).test(inputs.get(i))) continue;
            seen[t] = true;
            if (owner[t] < 0 || augment(owner[t], inputs, tests, owner, seen)) {
                owner[t] = i;
                return true;
            }
        }
        return false;
    }
}
