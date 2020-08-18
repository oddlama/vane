package org.oddlama.vane.core.command;

import java.util.function.Function;

public class FixedParam<T> implements Param {
	public FixedParam(T arg, Function<T, String> to_string) {
	}
}
