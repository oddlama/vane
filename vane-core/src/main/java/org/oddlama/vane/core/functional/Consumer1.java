package org.oddlama.vane.core.functional;

import java.util.List;

@FunctionalInterface
public interface Consumer1<T1> extends ErasedFunctor, GenericsFinder {
    void apply(T1 t1);

    @Override
    @SuppressWarnings("unchecked")
    public default Object invoke(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("Functor needs 1 arguments but got " + args.size() + " arguments");
        }
        apply((T1) args.get(0));
        return null;
    }
}
