package org.oddlama.vane.core.functional;

import java.util.List;

@FunctionalInterface
public interface Consumer4<T1, T2, T3, T4> extends ErasedFunctor, GenericsFinder {
    void apply(T1 t1, T2 t2, T3 t3, T4 t4);

    @Override
    @SuppressWarnings("unchecked")
    public default Object invoke(List<Object> args) {
        if (args.size() != 4) {
            throw new IllegalArgumentException("Functor needs 4 arguments but got " + args.size() + " arguments");
        }
        apply((T1) args.get(0), (T2) args.get(1), (T3) args.get(2), (T4) args.get(3));
        return null;
    }
}
