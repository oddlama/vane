package org.oddlama.vane.core.functional;

import java.util.List;

@FunctionalInterface
public interface Function1<T1, R> extends ErasedFunctor, GenericsFinder {
    R apply(T1 t1);

    @Override
    @SuppressWarnings("unchecked")
    public default Object invoke(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("Functor needs 1 arguments but got " + args.size() + " arguments");
        }
        return apply((T1) args.get(0));
    }
}
