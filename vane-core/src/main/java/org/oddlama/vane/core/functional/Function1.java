package org.oddlama.vane.core.functional;

@FunctionalInterface
public interface Function1<T1, R> extends GenericsFinder {
	R apply(T1 t1);
}
