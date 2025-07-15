package org.oddlama.vane.core.functional;

import java.util.List;

@FunctionalInterface
public interface Consumer3<T1, T2, T3> extends ErasedFunctor, GenericsFinder {
    void apply(T1 t1, T2 t2, T3 t3);

    @Override
    @SuppressWarnings("unchecked")
    public default Object invoke(List<Object> args) {
        if (args.size() != 3) {
            throw new IllegalArgumentException("Functor needs 3 arguments but got " + args.size() + " arguments");
        }
        apply((T1) args.get(0), (T2) args.get(1), (T3) args.get(2));
        return null;
    }
}
