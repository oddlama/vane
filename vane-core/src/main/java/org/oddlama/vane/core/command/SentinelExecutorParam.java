package org.oddlama.vane.core.command;

import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.functional.Function3;
import org.oddlama.vane.core.functional.Function4;
import org.oddlama.vane.core.functional.Function5;
import org.oddlama.vane.core.functional.Function6;
import java.util.function.Function;
import java.util.Collection;

public class SentinelExecutorParam<T> implements Param {
	T function;

	public SentinelExecutorParam(T function) {
		this.function = function;
	}

	public<T1> void exec(Function1<T1, Boolean> f) {
		throw new RuntimeException("Cannot add executor to sentinel executor!");
	}

	public<T1, T2> void exec(Function2<T1, T2, Boolean> f) {
		throw new RuntimeException("Cannot add executor to sentinel executor!");
	}

	public<T1, T2, T3> void exec(Function3<T1, T2, T3, Boolean> f) {
		throw new RuntimeException("Cannot add executor to sentinel executor!");
	}

	public<T1, T2, T3, T4> void exec(Function4<T1, T2, T3, T4, Boolean> f) {
		throw new RuntimeException("Cannot add executor to sentinel executor!");
	}

	public<T1, T2, T3, T4, T5> void exec(Function5<T1, T2, T3, T4, T5, Boolean> f) {
		throw new RuntimeException("Cannot add executor to sentinel executor!");
	}

	public<T1, T2, T3, T4, T5, T6> void exec(Function6<T1, T2, T3, T4, T5, T6, Boolean> f) {
		throw new RuntimeException("Cannot add executor to sentinel executor!");
	}

	public<T extends String> Param any_param() {
		throw new RuntimeException("Cannot add parameters to sentinel executor!");
	}

	public<T> Param any_param(Function<T, String> to_string) {
		throw new RuntimeException("Cannot add parameters to sentinel executor!");
	}

	public Param fixed_param(String fixed) {
		throw new RuntimeException("Cannot add parameters to sentinel executor!");
	}

	public<T> Param fixed_param(T fixed, Function<T, String> to_string) {
		throw new RuntimeException("Cannot add parameters to sentinel executor!");
	}

	public Param choice_param(Collection<String> choices) {
		throw new RuntimeException("Cannot add parameters to sentinel executor!");
	}

	public<T> Param choice_param(Collection<T> choices, Function<T, String> to_string) {
		throw new RuntimeException("Cannot add parameters to sentinel executor!");
	}
}
