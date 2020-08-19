package org.oddlama.vane.core.functional;

@FunctionalInterface
public interface Function3<T1, T2, T3, R> extends GenericsFinder {
	R apply(T1 t1, T2 t2, T3 t3);
}
