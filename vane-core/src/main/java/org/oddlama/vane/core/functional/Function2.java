package org.oddlama.vane.core.functional;

import java.util.List;

@FunctionalInterface
public interface Function2<T1, T2, R> extends ErasedFunctor, GenericsFinder {
    R apply(T1 t1, T2 t2);

    @Override
    @SuppressWarnings("unchecked")
    public default Object invoke(List<Object> args) {
        if (args.size() != 2) {
            throw new IllegalArgumentException("Functor needs 2 arguments but got " + args.size() + " arguments");
        }
        return apply((T1) args.get(0), (T2) args.get(1));
    }
}
