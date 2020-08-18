package org.oddlama.vane.core.command;

import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.functional.Function3;
import org.oddlama.vane.core.functional.Function4;
import org.oddlama.vane.core.functional.Function5;
import org.oddlama.vane.core.functional.Function6;
import org.bukkit.command.CommandSender;
import java.util.function.Function;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;


public interface Param {
	public List<Param> params = new ArrayList<>();

	default public<T1> void exec(Function1<T1, Boolean> f) {
		params.add(new SentinelExecutorParam<>(f));
	}

	default public<T1, T2> void exec(Function2<T1, T2, Boolean> f) {
		params.add(new SentinelExecutorParam<>(f));
	}

	default public<T1, T2, T3> void exec(Function3<T1, T2, T3, Boolean> f) {
		params.add(new SentinelExecutorParam<>(f));
	}

	default public<T1, T2, T3, T4> void exec(Function4<T1, T2, T3, T4, Boolean> f) {
		params.add(new SentinelExecutorParam<>(f));
	}

	default public<T1, T2, T3, T4, T5> void exec(Function5<T1, T2, T3, T4, T5, Boolean> f) {
		params.add(new SentinelExecutorParam<>(f));
	}

	default public<T1, T2, T3, T4, T5, T6> void exec(Function6<T1, T2, T3, T4, T5, T6, Boolean> f) {
		params.add(new SentinelExecutorParam<>(f));
	}

	default public<T extends String> Param any_param() {
		final var p = new AnyParam<T>(str -> str);
		params.add(p);
		return p;
	}

	default public<T> Param any_param(Function<T, String> to_string) {
		final var p = new AnyParam<T>(to_string);
		params.add(p);
		return p;
	}

	default public Param fixed_param(String fixed) {
		final var p = new FixedParam<String>(fixed, str -> str);
		params.add(p);
		return p;
	}

	default public<T> Param fixed_param(T fixed, Function<T, String> to_string) {
		final var p = new FixedParam<T>(fixed, to_string);
		params.add(p);
		return p;
	}

	default public Param choice_param(Collection<String> choices) {
		final var p = new ChoiceParam<String>(choices, str -> str);
		params.add(p);
		return p;
	}

	default public<T> Param choice_param(Collection<T> choices, Function<T, String> to_string) {
		final var p = new ChoiceParam<T>(choices, to_string);
		params.add(p);
		return p;
	}

	//public boolean accepts(String arg);
	//public boolean accept(String[] args);
}
