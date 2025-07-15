package org.oddlama.vane.core.functional;

import java.util.List;

public interface ErasedFunctor {
    public Object invoke(List<Object> args);
}
