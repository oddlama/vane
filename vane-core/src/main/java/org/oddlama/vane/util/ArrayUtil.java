package org.oddlama.vane.util;

import java.util.Arrays;

public class ArrayUtil {

    public static <T> T[] prepend(T[] arr, T element) {
        final var n = arr.length;
        arr = Arrays.copyOf(arr, n + 1);
        for (int i = arr.length - 1; i > 0; --i) {
            arr[i] = arr[i - 1];
        }
        arr[0] = element;
        return arr;
    }

    public static <T> T[] append(T[] arr, T element) {
        final var n = arr.length;
        arr = Arrays.copyOf(arr, n + 1);
        arr[n] = element;
        return arr;
    }
}
