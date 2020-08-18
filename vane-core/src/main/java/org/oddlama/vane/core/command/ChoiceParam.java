package org.oddlama.vane.core.command;

import java.util.function.Function;
import java.util.Collection;

public class ChoiceParam<T> implements Param {
	public ChoiceParam(Collection<T> choices, Function<T, String> to_string) {
	}
}
