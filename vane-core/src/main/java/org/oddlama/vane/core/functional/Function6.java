package org.oddlama.vane.core.functional;

import java.util.List;

@FunctionalInterface
public interface Function6<T1, T2, T3, T4, T5, T6, R> extends ErasedFunctor, GenericsFinder {
    R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);

    @Override
    @SuppressWarnings("unchecked")
    public default Object invoke(List<Object> args) {
        if (args.size() != 6) {
            throw new IllegalArgumentException("Functor needs 6 arguments but got " + args.size() + " arguments");
        }
        return apply(
            (T1) args.get(0),
            (T2) args.get(1),
            (T3) args.get(2),
            (T4) args.get(3),
            (T5) args.get(4),
            (T6) args.get(5)
        );
    }
}
