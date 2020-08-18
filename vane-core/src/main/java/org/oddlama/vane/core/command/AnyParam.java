package org.oddlama.vane.core.command;

import java.util.function.Function;

public class AnyParam<T> implements Param {
	public AnyParam(Function<T, String> to_string) {
	}
}
